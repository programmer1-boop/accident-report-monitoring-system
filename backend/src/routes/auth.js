const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const db = require('../config/db');
const { JWT_SECRET, authenticateToken } = require('../middleware/auth');

const router = express.Router();
const JWT_EXPIRES_IN = process.env.JWT_EXPIRES_IN || '7d';

/**
 * POST /api/auth/register
 * Public registration for Citizen accounts only.
 */
router.post('/register', async (req, res) => {
  const fullName = req.body.full_name || req.body.fullName || req.body.name;
  const userEmail = req.body.email || req.body.emailAddress;
  const userPhone = req.body.phone_number || req.body.phoneNumber || req.body.phone;
  const userPassword = req.body.password;

  if (!fullName || typeof fullName !== 'string' || !fullName.trim()) {
    return res.status(400).json({ error: 'Full name is required' });
  }

  if (!userEmail || typeof userEmail !== 'string' || !userEmail.trim() || !userEmail.includes('@')) {
    return res.status(400).json({ error: 'Valid email address is required' });
  }

  if (!userPassword || userPassword.length < 6) {
    return res.status(400).json({ error: 'Password must be at least 6 characters long' });
  }

  const normalizedEmail = userEmail.trim().toLowerCase();
  const normalizedPhone = userPhone ? String(userPhone).trim() : null;

  try {
    await db.query(`ALTER TABLE profiles ADD COLUMN IF NOT EXISTS avatar_url TEXT;`).catch(() => {});

    const existingCheck = await db.query(
      `SELECT id FROM profiles WHERE LOWER(email) = LOWER($1) OR (phone_number = $2 AND phone_number IS NOT NULL)`,
      [normalizedEmail, normalizedPhone]
    );

    if (existingCheck.rows.length > 0) {
      return res.status(409).json({ error: 'An account with this email address or mobile number already exists.' });
    }

    const passwordHash = await bcrypt.hash(userPassword, 12);

    const client = await db.pool.connect();
    try {
      await client.query('BEGIN');

      const profileResult = await client.query(
        `INSERT INTO profiles (full_name, email, phone_number, role, account_status)
         VALUES ($1, $2, $3, 'CITIZEN', 'ACTIVE')
         RETURNING id, full_name, email, phone_number, role, account_status, avatar_url, created_at`,
        [fullName.trim(), normalizedEmail, normalizedPhone]
      );

      const newProfile = profileResult.rows[0];

      await client.query(
        `INSERT INTO user_credentials (user_id, password_hash)
         VALUES ($1, $2)`,
        [newProfile.id, passwordHash]
      );

      await client.query(
        `INSERT INTO audit_logs (actor_profile_id, action, entity_type, entity_id, metadata)
         VALUES ($1, 'CITIZEN_REGISTERED', 'PROFILE', $1, $2)`,
        [newProfile.id, JSON.stringify({ email: normalizedEmail, phone: normalizedPhone })]
      );

      await client.query('COMMIT');

      const token = jwt.sign(
        { id: newProfile.id, role: newProfile.role },
        JWT_SECRET,
        { expiresIn: JWT_EXPIRES_IN }
      );

      return res.status(201).json({
        message: 'Registration successful',
        token,
        user: newProfile
      });
    } catch (txErr) {
      await client.query('ROLLBACK');
      throw txErr;
    } finally {
      client.release();
    }
  } catch (err) {
    if (err.code === '23505') {
      return res.status(409).json({ error: 'An account with this email address or mobile number already exists.' });
    }
    console.error('[ARMS Auth] Registration Error:', err);
    return res.status(500).json({ error: 'Internal server error during registration: ' + err.message });
  }
});

/**
 * POST /api/auth/login
 * User login verifying email/phone and password hash in PostgreSQL.
 */
router.post('/login', async (req, res) => {
  const { email, password } = req.body;

  if (!email || !password) {
    return res.status(400).json({ error: 'Email/Phone and password are required' });
  }

  const loginInput = email.trim();
  const normalizedEmail = loginInput.toLowerCase();

  try {
    await db.query(`ALTER TABLE profiles ADD COLUMN IF NOT EXISTS avatar_url TEXT;`).catch(() => {});

    const userResult = await db.query(
      `SELECT p.id, p.full_name, p.email, p.phone_number, p.role, p.account_status, p.avatar_url, c.password_hash
       FROM profiles p
       JOIN user_credentials c ON p.id = c.user_id
       WHERE LOWER(p.email) = LOWER($1) OR p.phone_number = $2`,
      [normalizedEmail, loginInput]
    );

    if (userResult.rows.length === 0) {
      return res.status(401).json({ error: 'Invalid email/phone or password' });
    }

    const user = userResult.rows[0];

    const isPasswordValid = await bcrypt.compare(password, user.password_hash);
    if (!isPasswordValid) {
      return res.status(401).json({ error: 'Invalid email/phone or password' });
    }

    if (user.account_status !== 'ACTIVE') {
      return res.status(403).json({ error: 'Account is suspended or deactivated. Contact ARMS administrator.' });
    }

    let agencyType = 'CDRRMO';
    if (user.role === 'RESPONDER') {
      const memb = await db.query(
        `SELECT t.team_type FROM responder_memberships m JOIN responder_teams t ON m.responder_team_id = t.id WHERE m.responder_profile_id = $1 AND m.is_active = TRUE LIMIT 1`,
        [user.id]
      );
      if (memb.rows.length > 0) {
        let tType = memb.rows[0].team_type;
        if (tType === 'MEDICAL_EMS') agencyType = 'MEDICAL';
        else agencyType = tType;
      }
    }

    const token = jwt.sign(
      { id: user.id, role: user.role },
      JWT_SECRET,
      { expiresIn: JWT_EXPIRES_IN }
    );

    const userProfile = {
      id: user.id,
      full_name: user.full_name,
      email: user.email,
      phone_number: user.phone_number,
      role: user.role,
      account_status: user.account_status,
      avatar_url: user.avatar_url,
      agency_type: agencyType
    };

    return res.json({
      message: 'Login successful',
      token,
      user: userProfile
    });
  } catch (err) {
    console.error('[ARMS Auth] Login Error:', err);
    return res.status(500).json({ error: 'Internal server error during login: ' + err.message });
  }
});

/**
 * POST /api/auth/avatar
 * Save profile picture avatar URL / base64 string to PostgreSQL database
 */
router.post('/avatar', authenticateToken, async (req, res) => {
  const { avatar_url } = req.body;
  const userId = req.user.id;

  if (!avatar_url || typeof avatar_url !== 'string') {
    return res.status(400).json({ error: 'Valid avatar data is required' });
  }

  try {
    await db.query(`ALTER TABLE profiles ADD COLUMN IF NOT EXISTS avatar_url TEXT;`).catch(() => {});

    await db.query(
      `UPDATE profiles SET avatar_url = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2`,
      [avatar_url, userId]
    );

    return res.json({ message: 'Profile picture saved to cloud database successfully' });
  } catch (err) {
    console.error('[ARMS Auth] Avatar Upload Error:', err);
    return res.status(500).json({ error: 'Internal server error while saving avatar' });
  }
});

/**
 * POST /api/auth/forgot-password
 * Request a password reset audit record for an existing account.
 */
router.post('/forgot-password', async (req, res) => {
  const identifier = req.body.email || req.body.username;
  if (!identifier || typeof identifier !== 'string' || !identifier.trim()) {
    return res.status(400).json({ error: 'Email or username is required' });
  }

  const normalized = identifier.trim().toLowerCase();
  try {
    const userRes = await db.query(
      `SELECT id, email FROM profiles WHERE LOWER(email) = $1 OR LOWER(email) = $2 LIMIT 1`,
      [normalized, `${normalized}@arms.gov.ph`]
    );

    if (userRes.rows.length > 0) {
      await db.query(
        `INSERT INTO audit_logs (actor_profile_id, action, entity_type, entity_id, metadata)
         VALUES ($1, 'PASSWORD_RESET_REQUESTED', 'PROFILE', $1, $2)`,
        [userRes.rows[0].id, JSON.stringify({ email: userRes.rows[0].email })]
      ).catch(() => {});
    }

    return res.json({
      message: 'If an account matches that email/username, a password reset request has been logged.'
    });
  } catch (err) {
    console.error('[ARMS Auth] Forgot Password Error:', err);
    return res.status(500).json({ error: 'Internal server error during password reset request' });
  }
});

module.exports = router;
