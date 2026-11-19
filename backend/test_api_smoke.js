/**
 * ARMS Backend API Smoke & Integration Test Script (Phase 9)
 * Run while `npm start` is active:
 *   node test_api_smoke.js
 */
const BASE_URL = process.env.ARMS_BASE_URL || 'http://localhost:5000/api';

async function request(method, path, body = null, token = null) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });
  const data = await res.json().catch(() => ({}));
  return { status: res.status, data };
}

async function run() {
  console.log(`[ARMS Smoke Test] Testing ${BASE_URL}...`);

  const health = await request('GET', '/health');
  console.log('1. GET /health ->', health.status, health.data.database || health.data);

  const guest = await request('POST', '/incidents/guest', {
    incident_type: 'ROAD_ACCIDENT',
    landmark_address: 'Sayre Highway, Poblacion, Valencia City, Bukidnon, Philippines',
    description: 'Automated smoke test report',
    latitude: 7.9064,
    longitude: 125.0942,
    location_accuracy: 5.0,
    selected_responders: ['CDRRMO', 'BFP'],
  });
  console.log('2. POST /incidents/guest ->', guest.status, guest.data.incident_reference || guest.data.error);

  if (guest.status === 201 && guest.data.tracking_token) {
    const track = await request('POST', '/incidents/guest/track', {
      incident_reference: guest.data.incident_reference,
      tracking_token: guest.data.tracking_token,
    });
    console.log('3. POST /incidents/guest/track ->', track.status);
  }

  const adminLogin = await request('POST', '/auth/login', {
    email: 'admin@arms.gov.ph',
    password: 'password123',
  });
  console.log('4. POST /auth/login (SUPER_ADMIN) ->', adminLogin.status);

  if (adminLogin.status === 200 && adminLogin.data.token) {
    const queue = await request('GET', '/dispatcher/queue', null, adminLogin.data.token);
    console.log('5. GET /dispatcher/queue ->', queue.status);

    const logs = await request('GET', '/admin/audit-logs?limit=5', null, adminLogin.data.token);
    console.log('6. GET /admin/audit-logs ->', logs.status);
  }

  const responderLogin = await request('POST', '/auth/login', {
    email: 'bfp@arms.gov.ph',
    password: 'password123',
  });
  console.log('7. POST /auth/login (RESPONDER BFP) ->', responderLogin.status);

  if (responderLogin.status === 200 && responderLogin.data.token) {
    const dispatches = await request('GET', '/responder/dispatches', null, responderLogin.data.token);
    console.log('8. GET /responder/dispatches ->', dispatches.status);
  }

  console.log('[ARMS Smoke Test] Completed.');
}

run().catch((err) => {
  console.error('[ARMS Smoke Test] Failed:', err.message);
  process.exit(1);
});
