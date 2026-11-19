const express = require('express');
const cors = require('cors');
const bcrypt = require('bcryptjs');
require('dotenv').config();

const db = require('./config/db');
const authRouter = require('./routes/auth');
const incidentRouter = require('./routes/incidents');
const dispatcherRouter = require('./routes/dispatcher');
const responderRouter = require('./routes/responder');
const adminRouter = require('./routes/admin');
const { authenticateToken, requireRole } = require('./middleware/auth');

const app = express();
const PORT = process.env.PORT || 5000;

const path = require('path');
const fs = require('fs');
const uploadsDir = path.join(__dirname, '..', 'uploads');
if (!fs.existsSync(uploadsDir)) {
  fs.mkdirSync(uploadsDir, { recursive: true });
}

app.use(cors());
app.use(express.json({ limit: '15mb' }));
app.use('/uploads', express.static(uploadsDir));

// API Routers
app.use('/api/auth', authRouter);
app.use('/api/incidents', incidentRouter);
app.use('/api/dispatcher', dispatcherRouter);
app.use('/api/responder', responderRouter);
app.use('/api/admin', adminRouter);

// Protected Test Route
app.get('/api/auth/me', authenticateToken, (req, res) => {
  res.json({
    status: 'AUTHENTICATED',
    user: req.user
  });
});

// Privileged Admin Test Route
app.get('/api/auth/admin-only', authenticateToken, requireRole('SUPER_ADMIN'), (req, res) => {
  res.json({
    status: 'AUTHORIZED',
    message: 'Welcome Super Admin',
    user: req.user
  });
});

// Health Check Endpoint
app.get('/api/health', async (req, res) => {
  let dbStatus = 'NOT_CONNECTED';
  try {
    await db.query('SELECT 1');
    dbStatus = 'CONNECTED';
  } catch (err) {
    dbStatus = 'UNAVAILABLE (Check .env credentials)';
  }

  res.json({
    status: 'ONLINE',
    system: 'ARMS — Accident Reports and Monitoring System',
    service_area: 'Valencia City, Bukidnon, Philippines',
    database: dbStatus,
    timestamp: new Date().toISOString()
  });
});

async function seedDefaultUsers() {
  try {
    await db.query(`ALTER TABLE profiles ADD COLUMN IF NOT EXISTS avatar_url TEXT;`).catch(() => {});
    await db.query(`ALTER TABLE responder_teams ADD COLUMN IF NOT EXISTS station_address TEXT;`).catch(() => {});

    // 1. Ensure 4 responder teams exist matching schema team_type constraints
    const teams = [
      { name: 'Valencia City CDRRMO Disaster Response Center', type: 'CDRRMO' },
      { name: 'Bureau of Fire Protection (BFP) Valencia Station', type: 'BFP' },
      { name: 'Philippine National Police (PNP) Valencia Station', type: 'PNP' },
      { name: 'Valencia City Hospital Medical Unit', type: 'MEDICAL_EMS' }
    ];

    const teamIds = {};
    for (const t of teams) {
      let teamRes = await db.query('SELECT id FROM responder_teams WHERE team_type = $1', [t.type]);
      if (teamRes.rows.length === 0) {
        const insRes = await db.query(
          `INSERT INTO responder_teams (team_name, team_type, is_active)
           VALUES ($1, $2, TRUE)
           RETURNING id`,
          [t.name, t.type]
        );
        teamIds[t.type] = insRes.rows[0].id;
      } else {
        teamIds[t.type] = teamRes.rows[0].id;
      }
    }

    // 2. Default accounts for testing all roles & agencies
    const defaultAccounts = [
      { name: 'Juan Citizen', email: 'citizen@arms.gov.ph', role: 'CITIZEN', agency: null },
      { name: 'Command Dispatcher', email: 'dispatcher@arms.gov.ph', role: 'DISPATCHER', agency: null },
      { name: 'System Administrator', email: 'admin@arms.gov.ph', role: 'SUPER_ADMIN', agency: null },
      { name: 'CDRRMO Disaster Officer', email: 'cdrrmo@arms.gov.ph', role: 'RESPONDER', agency: 'CDRRMO' },
      { name: 'BFP Fire Officer', email: 'bfp@arms.gov.ph', role: 'RESPONDER', agency: 'BFP' },
      { name: 'PNP Police Officer', email: 'pnp@arms.gov.ph', role: 'RESPONDER', agency: 'PNP' },
      { name: 'Medical ER Officer', email: 'medical@arms.gov.ph', role: 'RESPONDER', agency: 'MEDICAL_EMS' }
    ];

    const passwordHash = await bcrypt.hash('password123', 12);

    for (const acc of defaultAccounts) {
      const existing = await db.query('SELECT id FROM profiles WHERE LOWER(email) = LOWER($1)', [acc.email]);
      let userId;
      if (existing.rows.length === 0) {
        const pRes = await db.query(
          `INSERT INTO profiles (full_name, email, role, account_status) VALUES ($1, $2, $3, 'ACTIVE') RETURNING id`,
          [acc.name, acc.email, acc.role]
        );
        userId = pRes.rows[0].id;
        await db.query(
          `INSERT INTO user_credentials (user_id, password_hash) VALUES ($1, $2) ON CONFLICT (user_id) DO NOTHING`,
          [userId, passwordHash]
        );
        console.log(`[ARMS Seed] Created test account: ${acc.email} (${acc.role} - ${acc.agency || 'N/A'})`);
      } else {
        userId = existing.rows[0].id;
      }

      // Link responder to specific agency team
      if (acc.role === 'RESPONDER' && acc.agency && teamIds[acc.agency]) {
        await db.query(
          `INSERT INTO responder_memberships (responder_profile_id, responder_team_id, is_active)
           VALUES ($1, $2, TRUE)
           ON CONFLICT DO NOTHING`,
          [userId, teamIds[acc.agency]]
        );
      }
    }

    console.log('[ARMS Seed] All default accounts and responder teams seeded successfully (Zero dummy incident data).');
  } catch (seedErr) {
    console.error('[ARMS Seed] Notice during seeding:', seedErr.message);
  }
}

app.listen(PORT, '0.0.0.0', () => {
    console.log(`[ARMS Backend] Server running on port ${PORT}`);
    seedDefaultUsers();
});
