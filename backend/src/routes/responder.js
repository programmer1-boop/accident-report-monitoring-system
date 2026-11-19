const express = require('express');
const db = require('../config/db');
const { authenticateToken, requireRole } = require('../middleware/auth');

const router = express.Router();

const ALLOWED_RESPONDER_STATUSES = ['ACCEPTED', 'DECLINED', 'EN_ROUTE', 'ON_SCENE', 'RESOLVED'];

/**
 * GET /api/responder/dispatches
 * Retrieve active dispatches assigned to the authenticated responder's team and office.
 */
router.get('/dispatches', authenticateToken, requireRole('RESPONDER'), async (req, res) => {
  const responderId = req.user.id;

  try {
    // 1. Get responder's active team membership and office name
    const membershipResult = await db.query(
      `SELECT m.responder_team_id, t.team_name, t.team_type AS agency_type
       FROM responder_memberships m
       JOIN responder_teams t ON m.responder_team_id = t.id
       WHERE m.responder_profile_id = $1 AND m.is_active = TRUE`,
      [responderId]
    );

    if (membershipResult.rows.length === 0) {
      return res.json({
        message: 'No active office/team membership assigned to your profile',
        team_name: 'Valencia Emergency Operations Center',
        agency_type: 'CDRRMO',
        dispatches: []
      });
    }

    const team = membershipResult.rows[0];
    const teamId = team.responder_team_id;

    // 2. Retrieve active assignments for this specific office/team
    const dispatchesResult = await db.query(
      `SELECT a.id AS assignment_id, a.assignment_status, a.assigned_at, a.accepted_at, a.en_route_at, a.on_scene_at,
              i.id AS incident_id, i.incident_reference, i.incident_type, i.description, i.latitude, i.longitude, i.landmark_address, i.current_status
       FROM incident_assignments a
       JOIN incidents i ON a.incident_id = i.id
       WHERE a.responder_team_id = $1
         AND a.assignment_status IN ('DISPATCHED', 'ACCEPTED', 'EN_ROUTE', 'ON_SCENE')
       ORDER BY a.assigned_at DESC`,
      [teamId]
    );

    return res.json({
      team_id: teamId,
      team_name: team.team_name,
      agency_type: team.agency_type,
      dispatches: dispatchesResult.rows
    });
  } catch (err) {
    console.error('[ARMS Responder] Dispatches Error:', err);
    return res.status(500).json({ error: 'Internal server error while retrieving assigned dispatches' });
  }
});

/**
 * POST /api/responder/status
 * Progress assignment status (ACCEPT, DECLINE, EN_ROUTE, ON_SCENE, RESOLVED)
 */
router.post('/status', authenticateToken, requireRole('RESPONDER'), async (req, res) => {
  const { assignment_id, new_status, decline_reason } = req.body;

  if (!assignment_id || !new_status) {
    return res.status(400).json({ error: 'assignment_id and new_status are required' });
  }

  const normalizedStatus = new_status.toUpperCase();
  if (!ALLOWED_RESPONDER_STATUSES.includes(normalizedStatus)) {
    return res.status(400).json({
      error: `Invalid new_status. Must be one of: ${ALLOWED_RESPONDER_STATUSES.join(', ')}`
    });
  }

  if (normalizedStatus === 'DECLINED' && (!decline_reason || !decline_reason.trim())) {
    return res.status(400).json({ error: 'A valid decline_reason is required when declining a dispatch' });
  }

  const responderId = req.user.id;

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    const assignResult = await client.query(
      `SELECT a.id, a.incident_id, a.responder_team_id, a.assignment_status, i.current_status AS incident_current_status
       FROM incident_assignments a
       JOIN incidents i ON a.incident_id = i.id
       WHERE a.id = $1
       FOR UPDATE`,
      [assignment_id]
    );

    if (assignResult.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Assignment record not found' });
    }

    const assignment = assignResult.rows[0];

    const membershipCheck = await client.query(
      `SELECT id FROM responder_memberships WHERE responder_profile_id = $1 AND responder_team_id = $2 AND is_active = TRUE`,
      [responderId, assignment.responder_team_id]
    );

    if (membershipCheck.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(403).json({ error: 'Access denied. You do not belong to the assigned responder office.' });
    }

    if (!['DISPATCHED', 'ACCEPTED', 'EN_ROUTE', 'ON_SCENE'].includes(assignment.assignment_status)) {
      await client.query('ROLLBACK');
      return res.status(400).json({ error: `Cannot update assignment status. Current assignment status is ${assignment.assignment_status}` });
    }

    let incidentNewStatus = assignment.incident_current_status;
    let assignmentUpdateSql = '';
    let params = [];

    if (normalizedStatus === 'DECLINED') {
      assignmentUpdateSql = `UPDATE incident_assignments SET assignment_status = 'DECLINED', decline_reason = $1 WHERE id = $2`;
      params = [decline_reason.trim(), assignment_id];
      incidentNewStatus = 'VERIFIED';
    } else if (normalizedStatus === 'ACCEPTED') {
      assignmentUpdateSql = `UPDATE incident_assignments SET assignment_status = 'ACCEPTED', accepted_at = CURRENT_TIMESTAMP WHERE id = $1`;
      params = [assignment_id];
      incidentNewStatus = 'ACCEPTED';
    } else if (normalizedStatus === 'EN_ROUTE') {
      assignmentUpdateSql = `UPDATE incident_assignments SET assignment_status = 'EN_ROUTE', en_route_at = CURRENT_TIMESTAMP WHERE id = $1`;
      params = [assignment_id];
      incidentNewStatus = 'RESPONDING';
    } else if (normalizedStatus === 'ON_SCENE') {
      assignmentUpdateSql = `UPDATE incident_assignments SET assignment_status = 'ON_SCENE', on_scene_at = CURRENT_TIMESTAMP WHERE id = $1`;
      params = [assignment_id];
      incidentNewStatus = 'ON_SCENE';
    } else if (normalizedStatus === 'RESOLVED') {
      assignmentUpdateSql = `UPDATE incident_assignments SET assignment_status = 'RESOLVED', completed_at = CURRENT_TIMESTAMP WHERE id = $1`;
      params = [assignment_id];
      incidentNewStatus = 'RESOLVED';
    }

    await client.query(assignmentUpdateSql, params);

    await client.query(
      `UPDATE incidents SET current_status = $1 WHERE id = $2`,
      [incidentNewStatus, assignment.incident_id]
    );

    await client.query(
      `INSERT INTO incident_status_history (incident_id, previous_status, new_status, changed_by, change_reason)
       VALUES ($1, $2, $3, $4, $5)`,
      [assignment.incident_id, assignment.incident_current_status, incidentNewStatus, responderId, normalizedStatus === 'DECLINED' ? `Declined: ${decline_reason.trim()}` : `Office status: ${normalizedStatus}`]
    );

    await client.query(
      `INSERT INTO audit_logs (actor_profile_id, action, entity_type, entity_id, metadata)
       VALUES ($1, $2, 'ASSIGNMENT', $3, $4)`,
      [responderId, `RESPONDER_STATUS_${normalizedStatus}`, assignment_id, JSON.stringify({ new_status: normalizedStatus, incident_id: assignment.incident_id })]
    );

    await client.query('COMMIT');

    return res.json({
      message: `Assignment status successfully updated to ${normalizedStatus}`,
      assignment_id,
      incident_id: assignment.incident_id,
      assignment_status: normalizedStatus,
      incident_status: incidentNewStatus
    });
  } catch (txErr) {
    await client.query('ROLLBACK');
    console.error('[ARMS Responder] Status Update Error:', txErr);
    return res.status(500).json({ error: 'Internal server error while updating status' });
  } finally {
    client.release();
  }
});

/**
 * POST /api/responder/location
 * Update live responder GPS telemetry for an active incident so Citizens & Dispatchers can track arrival
 */
router.post('/location', authenticateToken, requireRole('RESPONDER'), async (req, res) => {
  const { incident_reference, latitude, longitude, agency_type } = req.body;
  const lat = parseFloat(latitude);
  const lng = parseFloat(longitude);

  if (!incident_reference || isNaN(lat) || isNaN(lng)) {
    return res.status(400).json({ error: 'incident_reference, latitude, and longitude are required' });
  }

  // Clamp strictly to Valencia City, Bukidnon, Philippines bounds
  const safeLat = (lat >= 7.75 && lat <= 8.05) ? lat : 7.9064;
  const safeLng = (lng >= 124.95 && lng <= 125.25) ? lng : 125.0942;

  try {
    await db.query(
      `CREATE TABLE IF NOT EXISTS responder_live_locations (
         incident_reference VARCHAR(100) PRIMARY KEY,
         responder_id UUID,
         agency_type VARCHAR(50),
         latitude NUMERIC(10, 7) NOT NULL,
         longitude NUMERIC(10, 7) NOT NULL,
         updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
       )`
    );

    await db.query(
      `INSERT INTO responder_live_locations (incident_reference, responder_id, agency_type, latitude, longitude, updated_at)
       VALUES ($1, $2, $3, $4, $5, CURRENT_TIMESTAMP)
       ON CONFLICT (incident_reference)
       DO UPDATE SET responder_id = EXCLUDED.responder_id,
                     agency_type = EXCLUDED.agency_type,
                     latitude = EXCLUDED.latitude,
                     longitude = EXCLUDED.longitude,
                     updated_at = CURRENT_TIMESTAMP`,
      [incident_reference.trim(), req.user.id, agency_type || 'RESPONDER', safeLat, safeLng]
    );

    return res.json({
      message: 'Responder live location updated',
      incident_reference: incident_reference.trim(),
      latitude: safeLat,
      longitude: safeLng
    });
  } catch (err) {
    console.error('[ARMS Responder] Live Location Error:', err);
    return res.status(500).json({ error: 'Internal server error while updating responder location' });
  }
});

module.exports = router;
