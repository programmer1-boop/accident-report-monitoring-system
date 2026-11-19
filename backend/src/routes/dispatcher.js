const express = require('express');
const db = require('../config/db');
const { authenticateToken, requireRole } = require('../middleware/auth');

const router = express.Router();

function haversineMeters(lat1, lon1, lat2, lon2) {
  const R = 6371000;
  const toRad = (deg) => (deg * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2);
  return 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

/**
 * GET /api/dispatcher/queue
 * Active incident queue for Dispatchers and Super Admins (with nearby duplicate detection).
 */
router.get('/queue', authenticateToken, requireRole('DISPATCHER', 'SUPER_ADMIN'), async (req, res) => {
  const page = parseInt(req.query.page || '1', 10);
  const limit = Math.min(parseInt(req.query.limit || '20', 10), 50);
  const offset = (Math.max(page, 1) - 1) * limit;

  try {
    const countResult = await db.query(
      `SELECT COUNT(*) FROM incidents WHERE current_status IN ('SUBMITTED', 'UNDER_REVIEW', 'VERIFIED', 'ASSIGNED', 'DISPATCHED')`
    );
    const totalQueueItems = parseInt(countResult.rows[0].count, 10);

    const queueResult = await db.query(
      `SELECT id, incident_reference, incident_type, description, latitude, longitude, landmark_address, current_status, submitted_at, created_at
       FROM incidents
       WHERE current_status IN ('SUBMITTED', 'UNDER_REVIEW', 'VERIFIED', 'ASSIGNED', 'DISPATCHED')
       ORDER BY created_at DESC
       LIMIT $1 OFFSET $2`,
      [limit, offset]
    );

    // Detect possible duplicates within 150 meters for same incident_type
    const rows = queueResult.rows.map((row) => ({ ...row, possible_duplicate: false }));
    for (let i = 0; i < rows.length; i++) {
      const a = rows[i];
      const latA = parseFloat(a.latitude);
      const lngA = parseFloat(a.longitude);
      if (isNaN(latA) || isNaN(lngA)) continue;

      for (let j = i + 1; j < rows.length; j++) {
        const b = rows[j];
        if ((a.incident_type || '').toUpperCase() !== (b.incident_type || '').toUpperCase()) continue;
        const latB = parseFloat(b.latitude);
        const lngB = parseFloat(b.longitude);
        if (isNaN(latB) || isNaN(lngB)) continue;

        const dist = Math.round(haversineMeters(latA, lngA, latB, lngB));
        if (dist <= 150) {
          a.possible_duplicate = true;
          a.duplicate_of_ref = b.incident_reference;
          a.duplicate_distance_m = dist;
          break;
        }
      }
    }

    return res.json({
      page,
      limit,
      total: totalQueueItems,
      queue: rows
    });
  } catch (err) {
    console.error('[ARMS Dispatcher] Queue Retrieval Error:', err);
    return res.status(500).json({ error: 'Internal server error while retrieving queue' });
  }
});

/**
 * POST /api/dispatcher/verify
 * Review and verify or reject an emergency incident.
 */
router.post('/verify', authenticateToken, requireRole('DISPATCHER', 'SUPER_ADMIN'), async (req, res) => {
  const { incident_id, verified, reason } = req.body;

  if (!incident_id) {
    return res.status(400).json({ error: 'incident_id is required' });
  }

  if (typeof verified !== 'boolean') {
    return res.status(400).json({ error: 'verified must be a boolean (true or false)' });
  }

  if (!verified && (!reason || !reason.trim())) {
    return res.status(400).json({ error: 'A valid reason is required when rejecting an incident' });
  }

  const newStatus = verified ? 'VERIFIED' : 'REJECTED';
  const actorId = req.user.id;

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    // 1. Lock incident for update
    const incidentCheck = await client.query(
      `SELECT id, current_status FROM incidents WHERE id = $1 FOR UPDATE`,
      [incident_id]
    );

    if (incidentCheck.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Incident not found' });
    }

    const previousStatus = incidentCheck.rows[0].current_status;

    // 2. Update incident status
    await client.query(
      `UPDATE incidents SET current_status = $1 WHERE id = $2`,
      [newStatus, incident_id]
    );

    // 3. Record status history
    await client.query(
      `INSERT INTO incident_status_history (incident_id, previous_status, new_status, changed_by, change_reason)
       VALUES ($1, $2, $3, $4, $5)`,
      [incident_id, previousStatus, newStatus, actorId, reason ? reason.trim() : (verified ? 'Report verified by dispatcher' : 'Report rejected')]
    );

    // 4. Audit log entry
    await client.query(
      `INSERT INTO audit_logs (actor_profile_id, action, entity_type, entity_id, metadata)
       VALUES ($1, $2, 'INCIDENT', $3, $4)`,
      [actorId, verified ? 'VERIFY_INCIDENT' : 'REJECT_INCIDENT', incident_id, JSON.stringify({ previousStatus, newStatus, reason })]
    );

    await client.query('COMMIT');

    return res.json({
      message: verified ? 'Incident report verified successfully' : 'Incident report rejected',
      incident_id,
      previous_status: previousStatus,
      current_status: newStatus
    });
  } catch (txErr) {
    await client.query('ROLLBACK');
    console.error('[ARMS Dispatcher] Verify Incident Error:', txErr);
    return res.status(500).json({ error: 'Internal server error while processing incident verification' });
  } finally {
    client.release();
  }
});

/**
 * POST /api/dispatcher/assign
 * Assign or reassign a verified incident to an authorized responder team.
 */
router.post('/assign', authenticateToken, requireRole('DISPATCHER', 'SUPER_ADMIN'), async (req, res) => {
  const { incident_id, responder_team_id } = req.body;

  if (!incident_id || !responder_team_id) {
    return res.status(400).json({ error: 'incident_id and responder_team_id are required' });
  }

  const actorId = req.user.id;

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    // 1. Lock incident
    const incidentCheck = await client.query(
      `SELECT id, current_status FROM incidents WHERE id = $1 FOR UPDATE`,
      [incident_id]
    );

    if (incidentCheck.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Incident not found' });
    }

    const currentIncidentStatus = incidentCheck.rows[0].current_status;

    if (currentIncidentStatus === 'REJECTED') {
      await client.query('ROLLBACK');
      return res.status(400).json({ error: 'Cannot assign a rejected incident report' });
    }

    // 2. Verify responder team exists
    const teamCheck = await client.query(
      `SELECT id, team_name FROM responder_teams WHERE id = $1`,
      [responder_team_id]
    );

    if (teamCheck.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Responder team not found' });
    }

    // 3. Mark previous active assignments as REASSIGNED
    await client.query(
      `UPDATE incident_assignments
       SET assignment_status = 'REASSIGNED'
       WHERE incident_id = $1 AND assignment_status IN ('DISPATCHED', 'ACCEPTED', 'EN_ROUTE', 'ON_SCENE')`,
      [incident_id]
    );

    // 4. Insert new dispatch assignment
    const assignResult = await client.query(
      `INSERT INTO incident_assignments (incident_id, responder_team_id, assigned_by, assignment_status)
       VALUES ($1, $2, $3, 'DISPATCHED')
       RETURNING id, assigned_at, assignment_status`,
      [incident_id, responder_team_id, actorId]
    );

    const newAssignment = assignResult.rows[0];

    // 5. Update incident status to ASSIGNED
    await client.query(
      `UPDATE incidents SET current_status = 'ASSIGNED' WHERE id = $1`,
      [incident_id]
    );

    // 6. Record status history & audit log
    await client.query(
      `INSERT INTO incident_status_history (incident_id, previous_status, new_status, changed_by, change_reason)
       VALUES ($1, $2, 'ASSIGNED', $3, $4)`,
      [incident_id, currentIncidentStatus, actorId, `Assigned to team ${teamCheck.rows[0].team_name}`]
    );

    await client.query(
      `INSERT INTO audit_logs (actor_profile_id, action, entity_type, entity_id, metadata)
       VALUES ($1, 'DISPATCH_INCIDENT', 'INCIDENT', $2, $3)`,
      [actorId, incident_id, JSON.stringify({ responder_team_id, team_name: teamCheck.rows[0].team_name })]
    );

    await client.query('COMMIT');

    return res.json({
      message: 'Incident successfully assigned to responder team',
      assignment_id: newAssignment.id,
      incident_id,
      responder_team_id,
      assignment_status: newAssignment.assignment_status,
      incident_status: 'ASSIGNED'
    });
  } catch (txErr) {
    await client.query('ROLLBACK');
    console.error('[ARMS Dispatcher] Assign Team Error:', txErr);
    return res.status(500).json({ error: 'Internal server error while assigning responder team' });
  } finally {
    client.release();
  }
});

module.exports = router;
