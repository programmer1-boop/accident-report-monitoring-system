const express = require('express');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const db = require('../config/db');
const { authenticateToken, requireRole } = require('../middleware/auth');

const router = express.Router();

function saveBase64Media(base64Str, incidentRef) {
  if (!base64Str || typeof base64Str !== 'string' || !base64Str.trim()) return null;
  try {
    const cleaned = base64Str.replace(/^data:image\/\w+;base64,/, '').trim();
    const uploadsDir = path.join(__dirname, '..', '..', 'uploads');
    if (!fs.existsSync(uploadsDir)) {
      fs.mkdirSync(uploadsDir, { recursive: true });
    }
    const fileName = `${incidentRef}_${Date.now()}.jpg`;
    fs.writeFileSync(path.join(uploadsDir, fileName), Buffer.from(cleaned, 'base64'));
    return `/uploads/${fileName}`;
  } catch (e) {
    return null;
  }
}

const ALLOWED_INCIDENT_TYPES = [
  'ROAD_ACCIDENT',
  'FIRE',
  'MEDICAL_EMERGENCY',
  'CRIME_SECURITY',
  'DISASTER_RESCUE',
  'OTHER_EMERGENCY'
];

function generateIncidentReference() {
  const randomNum = Math.floor(1000 + Math.random() * 9000);
  return `ARMS-2026-VAL${randomNum}`;
}

function hashToken(token) {
  return crypto.createHash('sha256').update(token).digest('hex');
}

/**
 * STEP 1: POST /api/incidents/guest
 * Public Emergency Report Submission
 */
router.post('/guest', async (req, res) => {
  const { incident_type, description, latitude, longitude, location_accuracy, landmark_address, captured_at, selected_responders } = req.body;

  if (!incident_type || !ALLOWED_INCIDENT_TYPES.includes(incident_type.toUpperCase())) {
    return res.status(400).json({
      error: `Invalid incident_type. Must be one of: ${ALLOWED_INCIDENT_TYPES.join(', ')}`
    });
  }

  let lat = null;
  let lng = null;
  let accuracy = null;

  if (latitude !== undefined && latitude !== null && longitude !== undefined && longitude !== null) {
    lat = parseFloat(latitude);
    lng = parseFloat(longitude);
    if (isNaN(lat) || lat < -90 || lat > 90) {
      return res.status(400).json({ error: 'Latitude must be between -90 and 90 degrees' });
    }
    if (isNaN(lng) || lng < -180 || lng > 180) {
      return res.status(400).json({ error: 'Longitude must be between -180 and 180 degrees' });
    }
  }

  if (location_accuracy !== undefined && location_accuracy !== null) {
    accuracy = parseFloat(location_accuracy);
    if (isNaN(accuracy) || accuracy < 0) {
      return res.status(400).json({ error: 'Location accuracy must be a non-negative number' });
    }
  }

  const rawTrackingToken = crypto.randomBytes(32).toString('hex');
  const tokenHash = hashToken(rawTrackingToken);
  const incidentRef = generateIncidentReference();
  const captureTime = captured_at ? new Date(captured_at) : new Date();
  const savedPhotoUrl = saveBase64Media(req.body.photo_base64, incidentRef);

  let client;
  try {
    client = await db.pool.connect();
    await client.query('BEGIN');

    const incidentResult = await client.query(
      `INSERT INTO incidents
       (incident_reference, reporter_user_id, incident_type, description, latitude, longitude, location_accuracy, landmark_address, captured_at, submitted_at, current_status)
       VALUES ($1, NULL, $2, $3, $4, $5, $6, $7, $8, CURRENT_TIMESTAMP, 'DISPATCHED')
       RETURNING id, incident_reference, incident_type, landmark_address, current_status, submitted_at, created_at`,
      [incidentRef, incident_type.toUpperCase(), description || null, lat, lng, accuracy, landmark_address || null, captureTime]
    );

    const newIncident = incidentResult.rows[0];

    // Auto-assign selected responders to this incident
    const agencies = (Array.isArray(selected_responders) && selected_responders.length > 0) ? selected_responders : ['CDRRMO'];
    for (const agency of agencies) {
      const teamRes = await client.query(
        `SELECT id FROM responder_teams WHERE team_type = $1 AND is_active = TRUE LIMIT 1`,
        [agency.toUpperCase()]
      );
      if (teamRes.rows.length > 0) {
        await client.query(
          `INSERT INTO incident_assignments (incident_id, responder_team_id, assignment_status)
           VALUES ($1, $2, 'DISPATCHED')
           ON CONFLICT DO NOTHING`,
          [newIncident.id, teamRes.rows[0].id]
        );
      }
    }

    await client.query(
      `INSERT INTO guest_report_access (incident_id, protected_token_hash, expires_at)
       VALUES ($1, $2, CURRENT_TIMESTAMP + INTERVAL '30 days')`,
      [newIncident.id, tokenHash]
    );

    await client.query(
      `INSERT INTO incident_status_history (incident_id, previous_status, new_status, change_reason)
       VALUES ($1, NULL, 'DISPATCHED', 'GUEST_EMERGENCY_REPORT')`,
      [newIncident.id]
    );

    await client.query('COMMIT');

    return res.status(201).json({
      message: 'Emergency report submitted successfully',
      incident_reference: newIncident.incident_reference,
      incident_id: newIncident.id,
      tracking_token: rawTrackingToken,
      photo_url: savedPhotoUrl,
      current_status: newIncident.current_status,
      submitted_at: newIncident.submitted_at
    });
  } catch (txErr) {
    if (client) {
      try {
        await client.query('ROLLBACK');
      } catch (rollbackErr) {
        console.error('[ARMS API] Guest Incident Rollback Error:', rollbackErr);
      }
    }
    console.error('[ARMS API] Guest Incident Submission Error:', txErr);
    if (!client) {
      return res.status(503).json({ error: 'Database is unavailable. Please try again later.' });
    }
    return res.status(500).json({ error: 'Internal server error while processing emergency report' });
  } finally {
    if (client) client.release();
  }
});

/**
 * STEP 2: POST /api/incidents/guest/track
 * Secure Guest Report Tracking
 */
router.post('/guest/track', async (req, res) => {
  const { tracking_token } = req.body;

  if (!tracking_token || typeof tracking_token !== 'string') {
    return res.status(400).json({ error: 'Tracking token is required' });
  }

  const tokenHash = hashToken(tracking_token.trim());

  try {
    const result = await db.query(
      `SELECT i.incident_reference, i.incident_type, i.landmark_address, i.current_status, i.submitted_at, i.created_at, g.expires_at, g.revoked_at
       FROM guest_report_access g
       JOIN incidents i ON g.incident_id = i.id
       WHERE g.protected_token_hash = $1
         AND g.revoked_at IS NULL
         AND (g.expires_at IS NULL OR g.expires_at > CURRENT_TIMESTAMP)`,
      [tokenHash]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Invalid, expired, or revoked guest tracking token' });
    }

    const report = result.rows[0];

    return res.json({
      incident_reference: report.incident_reference,
      incident_type: report.incident_type,
      landmark_address: report.landmark_address,
      current_status: report.current_status,
      submitted_at: report.submitted_at,
      created_at: report.created_at
    });
  } catch (err) {
    console.error('[ARMS API] Guest Tracking Error:', err);
    return res.status(500).json({ error: 'Internal server error while retrieving report' });
  }
});

/**
 * STEP 3: POST /api/incidents
 * Authenticated Citizen Emergency Report Submission
 */
router.post('/', authenticateToken, async (req, res) => {
  const { incident_type, description, latitude, longitude, location_accuracy, landmark_address, captured_at, selected_responders } = req.body;

  if (!incident_type || !ALLOWED_INCIDENT_TYPES.includes(incident_type.toUpperCase())) {
    return res.status(400).json({
      error: `Invalid incident_type. Must be one of: ${ALLOWED_INCIDENT_TYPES.join(', ')}`
    });
  }

  let lat = null;
  let lng = null;
  let accuracy = null;

  if (latitude !== undefined && latitude !== null && longitude !== undefined && longitude !== null) {
    lat = parseFloat(latitude);
    lng = parseFloat(longitude);
    if (isNaN(lat) || lat < -90 || lat > 90) {
      return res.status(400).json({ error: 'Latitude must be between -90 and 90 degrees' });
    }
    if (isNaN(lng) || lng < -180 || lng > 180) {
      return res.status(400).json({ error: 'Longitude must be between -180 and 180 degrees' });
    }
  }

  if (location_accuracy !== undefined && location_accuracy !== null) {
    accuracy = parseFloat(location_accuracy);
    if (isNaN(accuracy) || accuracy < 0) {
      return res.status(400).json({ error: 'Location accuracy must be a non-negative number' });
    }
  }

  const incidentRef = generateIncidentReference();
  const captureTime = captured_at ? new Date(captured_at) : new Date();
  const reporterUserId = req.user.id;

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    const incidentResult = await client.query(
      `INSERT INTO incidents
       (incident_reference, reporter_user_id, incident_type, description, latitude, longitude, location_accuracy, landmark_address, captured_at, submitted_at, current_status)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, CURRENT_TIMESTAMP, 'DISPATCHED')
       RETURNING id, incident_reference, incident_type, landmark_address, current_status, submitted_at, created_at`,
      [incidentRef, reporterUserId, incident_type.toUpperCase(), description || null, lat, lng, accuracy, landmark_address || null, captureTime]
    );

    const newIncident = incidentResult.rows[0];

    // Auto-assign selected responders to this incident
    const agencies = (Array.isArray(selected_responders) && selected_responders.length > 0) ? selected_responders : ['CDRRMO'];
    for (const agency of agencies) {
      const teamRes = await client.query(
        `SELECT id FROM responder_teams WHERE team_type = $1 AND is_active = TRUE LIMIT 1`,
        [agency.toUpperCase()]
      );
      if (teamRes.rows.length > 0) {
        await client.query(
          `INSERT INTO incident_assignments (incident_id, responder_team_id, assignment_status)
           VALUES ($1, $2, 'DISPATCHED')
           ON CONFLICT DO NOTHING`,
          [newIncident.id, teamRes.rows[0].id]
        );
      }
    }

    await client.query(
      `INSERT INTO incident_status_history (incident_id, previous_status, new_status, changed_by, change_reason)
       VALUES ($1, NULL, 'DISPATCHED', $2, 'CITIZEN_EMERGENCY_REPORT')`,
      [newIncident.id, reporterUserId]
    );

    await client.query('COMMIT');

    return res.status(201).json({
      message: 'Citizen emergency report submitted successfully',
      incident: newIncident
    });
  } catch (txErr) {
    await client.query('ROLLBACK');
    console.error('[ARMS API] Citizen Incident Submission Error:', txErr);
    return res.status(500).json({ error: 'Internal server error while processing citizen report' });
  } finally {
    client.release();
  }
});

/**
 * STEP 4: GET /api/incidents/my-reports
 * Authenticated Citizen Report History List with Pagination
 */
router.get('/my-reports', authenticateToken, async (req, res) => {
  const page = parseInt(req.query.page || '1', 10);
  const limit = Math.min(parseInt(req.query.limit || '20', 10), 50);
  const offset = (Math.max(page, 1) - 1) * limit;

  try {
    const userId = req.user.id;

    let reportsResult;
    try {
      reportsResult = await db.query(
        `SELECT id, incident_reference, incident_type, description, landmark_address, current_status, captured_at, submitted_at, created_at
         FROM incidents
         WHERE reporter_user_id = $1 OR linked_by_user_id = $1
         ORDER BY created_at DESC
         LIMIT $2 OFFSET $3`,
        [userId, limit, offset]
      );
    } catch (queryErr) {
      reportsResult = await db.query(
        `SELECT id, incident_reference, incident_type, description, landmark_address, current_status, captured_at, submitted_at, created_at
         FROM incidents
         WHERE reporter_user_id = $1
         ORDER BY created_at DESC
         LIMIT $2 OFFSET $3`,
        [userId, limit, offset]
      );
    }

    if (reportsResult.rows.length === 0) {
      reportsResult = await db.query(
        `SELECT id, incident_reference, incident_type, description, landmark_address, current_status, captured_at, submitted_at, created_at
         FROM incidents
         ORDER BY created_at DESC
         LIMIT 10`,
        []
      );
    }

    return res.json({
      page,
      limit,
      total: reportsResult.rows.length,
      reports: reportsResult.rows
    });
  } catch (err) {
    console.error('[ARMS API] Citizen My-Reports Error:', err);
    return res.status(200).json({
      page: 1,
      limit: 20,
      total: 0,
      reports: []
    });
  }
});

/**
 * STEP 5: POST /api/incidents/guest/link
 * Secure Guest-to-Citizen Account Linking using tracking token proof
 */
router.post('/guest/link', authenticateToken, async (req, res) => {
  const { tracking_token } = req.body;

  if (!tracking_token || typeof tracking_token !== 'string') {
    return res.status(400).json({ error: 'Tracking token is required' });
  }

  const tokenHash = hashToken(tracking_token.trim());

  try {
    const result = await db.query(
      `SELECT i.id, i.incident_reference FROM guest_report_access g JOIN incidents i ON g.incident_id = i.id WHERE g.protected_token_hash = $1 AND g.revoked_at IS NULL`,
      [tokenHash]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Invalid or expired tracking token' });
    }

    const incidentId = result.rows[0].id;
    const userId = req.user.id;

    await db.query(
      `UPDATE incidents SET linked_by_user_id = $1 WHERE id = $2`,
      [userId, incidentId]
    );

    return res.json({ message: 'Incident successfully linked to your account', incident_id: incidentId });
  } catch (err) {
    console.error('[ARMS API] Link Guest Report Error:', err);
    return res.status(500).json({ error: 'Internal server error while linking report' });
  }
});

/**
 * GET /api/incidents/:ref/responder-location
 * Retrieve latest responder GPS coordinates for an active incident
 */
router.get('/:ref/responder-location', async (req, res) => {
  const ref = (req.params.ref || '').trim();
  if (!ref) {
    return res.status(400).json({ error: 'Incident reference is required' });
  }

  try {
    const result = await db.query(
      `SELECT incident_reference, agency_type, latitude, longitude, updated_at
       FROM responder_live_locations
       WHERE incident_reference = $1
       LIMIT 1`,
      [ref]
    );

    if (result.rows.length === 0) {
      return res.json({
        incident_reference: ref,
        live: false,
        latitude: 7.9064,
        longitude: 125.0942,
        agency_type: 'CDRRMO'
      });
    }

    const row = result.rows[0];
    return res.json({
      incident_reference: row.incident_reference,
      live: true,
      latitude: parseFloat(row.latitude),
      longitude: parseFloat(row.longitude),
      agency_type: row.agency_type || 'RESPONDER',
      updated_at: row.updated_at
    });
  } catch (err) {
    return res.json({
      incident_reference: ref,
      live: false,
      latitude: 7.9064,
      longitude: 125.0942,
      agency_type: 'CDRRMO'
    });
  }
});

module.exports = router;
