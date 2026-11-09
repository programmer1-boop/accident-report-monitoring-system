const jwt = require('jsonwebtoken');
const db = require('../config/db');

const JWT_SECRET = process.env.JWT_SECRET || 'arms_default_jwt_secret_valencia_2026';

/**
 * Middleware: Verify JWT Bearer Token and load current user account status from database.
 */
async function authenticateToken(req, res, next) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.startsWith('Bearer ') ? authHeader.split(' ')[1] : null;

  if (!token) {
    return res.status(401).json({ error: 'Authentication token missing or invalid' });
  }

  try {
    const decoded = jwt.verify(token, JWT_SECRET);

    await db.query(`ALTER TABLE profiles ADD COLUMN IF NOT EXISTS avatar_url TEXT;`).catch(() => {});

    // Verify current account status and role from server-side database
    const userResult = await db.query(
      'SELECT id, full_name, email, phone_number, role, account_status, avatar_url FROM profiles WHERE id = $1',
      [decoded.id]
    );

    if (userResult.rows.length === 0) {
      return res.status(401).json({ error: 'User account no longer exists' });
    }

    const user = userResult.rows[0];

    if (user.account_status !== 'ACTIVE') {
      return res.status(403).json({ error: 'Account is suspended or deactivated. Contact ARMS administrator.' });
    }

    req.user = user;
    next();
  } catch (err) {
    if (err.name === 'TokenExpiredError') {
      return res.status(401).json({ error: 'Authentication token has expired' });
    }
    return res.status(401).json({ error: 'Authentication token is invalid' });
  }
}

/**
 * Middleware: Enforce server-side role authorization.
 */
function requireRole(...allowedRoles) {
  return (req, res, next) => {
    if (!req.user || !allowedRoles.includes(req.user.role)) {
      return res.status(403).json({ error: 'Access denied. Required role not authorized.' });
    }
    next();
  };
}

module.exports = {
  authenticateToken,
  requireRole,
  JWT_SECRET
};
