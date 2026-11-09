const express = require('express');
const bcrypt = require('bcryptjs');
const db = require('../config/db');
const { authenticateToken, requireRole } = require('../middleware/auth');

const router = express.Router();

const ALLOWED_STAFF_ROLES = ['RESPONDER', 'DISPATCHER'];
const ALLOWED_ACCOUNT_STATUSES = ['ACTIVE', 'SUSPENDED', 'DEACTIVATED'];
const ALLOWED_TEAM_TYPES = ['CDRRMO', 'BFP', 'PNP', 'MEDICAL_EMS', 'RED_CROSS', 'OTHER'];

/**
 * POST /api/admin/provision-staff
 * Provision a new RESPONDER or DISPATCHER account (Super Admin only)
 */
router.post('/provision-staff', authenticateToken, requireRole('SUPER_ADMIN'), async (req, res) => {
  const { full_name, email, phone_number, role, password, responder_team_id } = req.body;

  if (!full_name || !full_name.trim()) {
    return res.status(400).json({ error: 'Full name is required' });
  }

  if (!email || !email.trim() || !email.includes('@')) {
    return res.status(400).json({ error: 'Valid email address is required' });
  }

  if (!password || password.length < 6) {
    return res.status(400).json({ error: 'Password must be at least 6 characters long' });
  }

  const normalizedRole = role ? role.toUpperCase().trim() : '';
  if (!ALLOWED_STAFF_ROLES.includes(normalizedRole)) {
    return res.status(400).json({
      error: `Invalid staff role. Staff provisioning can only create: ${ALLOWED_STAFF_ROLES.join(', ')}`
    });
  }

  const normalizedEmail = email.trim().toLowerCase();
  const normalizedPhone = phone_number ? phone_number.trim() : null;
  const adminActorId = req.user.id;

  // Check duplicate email
  const existing = await db.query('SELECT id FROM profiles WHERE LOWER(email) = LOWER($1)', [normalizedEmail]);
  if (existing.rows.length > 0) {
    return res.status(409).json({ error: 'An account with this email address already exists.' });
  }

  // Hash password with cost factor 12
  const passwordHash = await bcrypt.hash(password, 12);

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    // 1. Insert Profile
    const profileResult = await client.query(
      `INSERT INTO profiles (full_name, email, phone_number, role, account_status)
       VALUES ($1, $2, $3, $4, 'ACTIVE')
       RETURNING id, full_name, email, phone_number, role, account_status, created_at`,
      [full_name.trim(), normalizedEmail, normalizedPhone, normalizedRole]
    );

    const newStaff = profileResult.rows[0];

    // 2. Insert User Credentials
    await client.query(
      `INSERT INTO user_credentials (user_id, password_hash)
       VALUES ($1, $2)`,
      [newStaff.id, passwordHash]
    );

    // 3. If Responder and team specified, insert team membership
    if (normalizedRole === 'RESPONDER' && responder_team_id) {
      const teamCheck = await client.query('SELECT id FROM responder_teams WHERE id = $1', [responder_team_id]);
      if (teamCheck.rows.length > 0) {
        await client.query(
          `INSERT INTO responder_memberships (responder_profile_id, responder_team_id, is_active)
           VALUES ($1, $2, TRUE)`,
          [newStaff.id, responder_team_id]
        );
      }
    }

    // 4. Audit Log
    await client.query(
      `INSERT INTO audit_logs (actor_profile_id, action, entity_type, entity_id, metadata)
       VALUES ($1, 'PROVISION_STAFF_ACCOUNT', 'PROFILE', $2, $3)`,
      [adminActorId, newStaff.id, JSON.stringify({ role: normalizedRole, email: normalizedEmail, responder_team_id })]
    );

    await client.query('COMMIT');

    return res.status(201).json({
      message: `Staff account successfully provisioned as ${normalizedRole}`,
      staff: newStaff
    });
  } catch (txErr) {
    await client.query('ROLLBACK');
    console.error('[ARMS Admin] Provision Staff Error:', txErr);
    return res.status(500).json({ error: 'Internal server error while provisioning staff account' });
  } finally {
    client.release();
  }
});

/**
 * POST /api/admin/account-status
 * Activate, suspend, or deactivate user accounts (Super Admin only)
 */
router.post('/account-status', authenticateToken, requireRole('SUPER_ADMIN'), async (req, res) => {
  const { profile_id, account_status } = req.body;

  if (!profile_id || !account_status) {
    return res.status(400).json({ error: 'profile_id and account_status are required' });
  }

  const normalizedStatus = account_status.toUpperCase().trim();
  if (!ALLOWED_ACCOUNT_STATUSES.includes(normalizedStatus)) {
    return res.status(400).json({
      error: `Invalid account_status. Must be one of: ${ALLOWED_ACCOUNT_STATUSES.join(', ')}`
    });
  }

  const adminActorId = req.user.id;

  // Safeguard 1: Self-deactivation protection
  if (profile_id === adminActorId) {
    return res.status(400).json({ error: 'You cannot suspend or deactivate your own Super Admin account' });
  }

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    // Lock target profile
    const targetCheck = await client.query(
      `SELECT id, role, account_status FROM profiles WHERE id = $1 FOR UPDATE`,
      [profile_id]
    );

    if (targetCheck.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'User profile not found' });
    }

    const targetUser = targetCheck.rows[0];

    // Safeguard 2: Last Active Super Admin Protection
    if (targetUser.role === 'SUPER_ADMIN' && (normalizedStatus === 'SUSPENDED' || normalizedStatus === 'DEACTIVATED')) {
      const activeAdminCount = await client.query(
        `SELECT COUNT(*) FROM profiles WHERE role = 'SUPER_ADMIN' AND account_status = 'ACTIVE' AND id != $1`,
        [profile_id]
      );

      const remainingAdmins = parseInt(activeAdminCount.rows[0].count, 10);
      if (remainingAdmins === 0) {
        await client.query('ROLLBACK');
        return res.status(400).json({ error: 'Cannot deactivate or suspend the last remaining active Super Admin account' });
      }
    }

    // Update account status
    await client.query(
      `UPDATE profiles SET account_status = $1 WHERE id = $2`,
      [normalizedStatus, profile_id]
    );

    // Audit Log Entry
    await client.query(
      `INSERT INTO audit_logs (actor_profile_id, action, entity_type, entity_id, metadata)
       VALUES ($1, 'UPDATE_ACCOUNT_STATUS', 'PROFILE', $2, $3)`,
      [adminActorId, profile_id, JSON.stringify({ previousStatus: targetUser.account_status, newStatus: normalizedStatus })]
    );

    await client.query('COMMIT');

    return res.json({
      message: `Account status updated to ${normalizedStatus}`,
      profile_id,
      account_status: normalizedStatus
    });
  } catch (txErr) {
    await client.query('ROLLBACK');
    console.error('[ARMS Admin] Account Status Update Error:', txErr);
    return res.status(500).json({ error: 'Internal server error while updating account status' });
  } finally {
    client.release();
  }
});

/**
 * POST /api/admin/teams
 * Create or manage responder agency teams (Super Admin only)
 */
router.post('/teams', authenticateToken, requireRole('SUPER_ADMIN'), async (req, res) => {
  const { team_name, team_type, description, is_active } = req.body;

  if (!team_name || !team_name.trim()) {
    return res.status(400).json({ error: 'team_name is required' });
  }

  const normalizedType = team_type ? team_type.toUpperCase().trim() : 'OTHER';
  if (!ALLOWED_TEAM_TYPES.includes(normalizedType)) {
    return res.status(400).json({
      error: `Invalid team_type. Must be one of: ${ALLOWED_TEAM_TYPES.join(', ')}`
    });
  }

  const adminActorId = req.user.id;
  const activeState = typeof is_active === 'boolean' ? is_active : false;

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    const result = await client.query(
      `INSERT INTO responder_teams (team_name, team_type, description, is_active)
       VALUES ($1, $2, $3, $4)
       ON CONFLICT (team_name) DO UPDATE
       SET team_type = EXCLUDED.team_type,
           description = EXCLUDED.description,
           is_active = EXCLUDED.is_active
       RETURNING id, team_name, team_type, description, is_active, created_at`,
      [team_name.trim(), normalizedType, description ? description.trim() : null, activeState]
    );

    const team = result.rows[0];

    await client.query(
      `INSERT INTO audit_logs (actor_profile_id, action, entity_type, entity_id, metadata)
       VALUES ($1, 'MANAGE_RESPONDER_TEAM', 'RESPONDER_TEAM', $2, $3)`,
      [adminActorId, team.id, JSON.stringify({ team_name: team.team_name, is_active: team.is_active })]
    );

    await client.query('COMMIT');

    return res.json({
      message: 'Responder team created or updated successfully',
      team
    });
  } catch (txErr) {
    await client.query('ROLLBACK');
    console.error('[ARMS Admin] Manage Team Error:', txErr);
    return res.status(500).json({ error: 'Internal server error while managing responder team' });
  } finally {
    client.release();
  }
});

/**
 * GET /api/admin/audit-logs
 * Paginated system audit trail viewer (Super Admin only)
 */
router.get('/audit-logs', authenticateToken, requireRole('SUPER_ADMIN'), async (req, res) => {
  const page = parseInt(req.query.page || '1', 10);
  const limit = Math.min(parseInt(req.query.limit || '20', 10), 100);
  const offset = (Math.max(page, 1) - 1) * limit;

  try {
    const countResult = await db.query('SELECT COUNT(*) FROM audit_logs');
    const totalLogs = parseInt(countResult.rows[0].count, 10);

    const logsResult = await db.query(
      `SELECT a.id, a.action, a.entity_type, a.entity_id, a.metadata, a.created_at, p.full_name AS actor_name, p.role AS actor_role
       FROM audit_logs a
       LEFT JOIN profiles p ON a.actor_profile_id = p.id
       ORDER BY a.created_at DESC
       LIMIT $1 OFFSET $2`,
      [limit, offset]
    );

    return res.json({
      page,
      limit,
      total: totalLogs,
      logs: logsResult.rows
    });
  } catch (err) {
    console.error('[ARMS Admin] Audit Logs Error:', err);
    return res.status(500).json({ error: 'Internal server error while retrieving audit logs' });
  }
});

module.exports = router;
