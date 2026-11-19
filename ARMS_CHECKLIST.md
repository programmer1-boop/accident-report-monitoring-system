# ARMS DEVELOPMENT CHECKLIST
**Based on:** `ARMS_MASTER_PLAN.md`  
**Project:** Accident Reports and Monitoring System (Valencia City, Bukidnon)  
**Legend:** `[x]` Implemented in codebase · `[ ]` Pending / Partial / Needs Fix or Verification

---

## PHASE 1 — Master Plan and Project Inspection
- [x] Finalize `ARMS_MASTER_PLAN.md`
- [x] Inspect the Android project (`app/` Java + XML structure)
- [x] Inspect the Node.js backend (`backend/src/` Express + routes)
- [x] Inspect PostgreSQL configuration and existing tables (`backend/migrations/001_initial_schema.sql`)
- [x] Confirm the existing framework and dependencies (Gradle, CameraX, Express, `pg`, `bcryptjs`, `jsonwebtoken`)
- [x] Identify existing and missing components
- [x] Propose the exact implementation order and document architectural decisions / unresolved questions

---

## PHASE 2 — Project Foundation
- [x] Configure the Android project (`build.gradle.kts`, `AndroidManifest.xml`, `App.java`)
- [x] Confirm Java and Gradle configuration (Java 11, `minSdk 23`, `targetSdk 37`)
- [x] Organize package structure (`data/model/`, `data/repository/` added alongside existing `fragments/`, `remote/`, `utils/`)
- [x] Configure XML themes and reusable resources (`colors.xml`, `dimens.xml`, `strings.xml`, `themes.xml`)
- [x] Establish screen navigation (`SplashActivity` → `WelcomeActivity` → Role Dashboards)
- [x] Inspect and organize the Node.js backend (`backend/src/server.js`, `config/`, `middleware/`, `routes/`)
- [x] Prepare backend configuration template (`backend/.env.example`)
- [x] Verify that the Android application and backend + PostgreSQL can start and connect end-to-end (`backend/.env` & `arms_db` configured)

---

## PHASE 3 — UI and Navigation
- [x] Build core public, guest, citizen, responder, and admin screens (Activities & Fragments)
- [x] Connect navigation across `SplashActivity`, `WelcomeActivity`, `CitizenDashboardActivity`, `ResponderDashboardActivity`, and `AdminDashboardActivity`
- [x] Connect all buttons to real functional actions (`ForgotPasswordActivity`, `AdminIncidentRecordsActivity`, `ResponderStatusUpdateActivity`, `DispatcherDashboardActivity`, `GuestReportTrackingActivity`)
- [x] Implement consistent loading, error, empty, and success states across all API-backed screens
- [x] Replace temporary in-memory/local stores with live API data (with offline/demo fallback)
- [ ] Test the full screen-navigation flow on emulator/device

---

## PHASE 4 — Node.js API and PostgreSQL
- [x] Confirm backend framework (Express.js in `backend/src/server.js`)
- [x] Prepare PostgreSQL schema and initial migration (`backend/migrations/001_initial_schema.sql`)
- [x] Configure database connection pool (`backend/src/config/db.js`)
- [x] Implement core API routes (`/api/auth`, `/api/incidents`, `/api/dispatcher`, `/api/responder`, `/api/admin`)
- [x] Implement JWT authentication & role middleware (`backend/src/middleware/auth.js`)
- [x] Fix SQL column mismatch in `backend/src/routes/responder.js` (`t.agency_type` → `t.team_type`)
- [x] Align role names (`ADMIN` / `SUPER_ADMIN`, `DISPATCHER`, `RESPONDER`, `CITIZEN`) and status lifecycle values across Android, API, and PostgreSQL
- [x] Configure protected media storage and upload handling (`backend/uploads/` + `photo_url` in `incidents.js`)
- [x] Implement password reset endpoint (`POST /api/auth/forgot-password` + `ForgotPasswordActivity`)
- [ ] Test all API and database operations against a live PostgreSQL instance

---

## PHASE 5 — Guest Emergency Reporting
- [x] Implement in-app photo capture and video recording (`GuestReportActivity`, `CameraCaptureActivity`)
- [x] Restrict gallery uploads for new incident evidence (camera-only capture)
- [x] Implement manual incident type selection (`IncidentLocationActivity`)
- [x] Implement live GPS coordinates, Leaflet map preview, and reverse geocoding (`IncidentLocationActivity`)
- [x] Implement full-resolution photo capture (`FileProvider` in `GuestReportActivity`) and backend `uploads/` storage
- [x] Ensure reports submit with `idempotency_key` and save `tracking_token` via `POST /api/incidents/guest`
- [x] Implement secure Guest Report Tracking UI (`GuestReportTrackingActivity` + Welcome screen entry)
- [ ] Verify dispatchers can retrieve submitted guest reports from PostgreSQL

---

## PHASE 6 — Citizen Features
- [x] Build Citizen registration and sign-in screens (`CitizenRegisterActivity`, `CitizenLoginActivity`)
- [x] Fix `CitizenRegisterActivity` payload (`email` vs `username`) so backend `POST /api/auth/register` succeeds
- [x] Fix `CitizenLoginActivity` to authenticate against `POST /api/auth/login` with real password verification and JWT storage (with offline fallback)
- [x] Implement Citizen Dashboard (`CitizenDashboardActivity`, `CitizenHomeFragment`, `CitizenReportsFragment`, `CitizenAlertsFragment`, `CitizenProfileFragment`)
- [x] Connect `CitizenReportsFragment` to live backend API data (`GET /api/incidents/my-reports` with offline fallback)
- [x] Fix logged-in citizen report endpoint in `IncidentLocationActivity` (`"/"` → `"/incidents"`)
- [x] Implement secure guest-to-account linking (`POST /api/incidents/guest/link` in `GuestReportTrackingActivity`)
- [ ] Test citizen access permissions and report ownership isolation

---

## PHASE 7 — Dispatcher and Responder Features
- [x] Implement backend dispatcher routes (`backend/src/routes/dispatcher.js`)
- [x] Build dedicated Dispatcher UI (`DispatcherDashboardActivity`, `DispatcherIncidentReviewActivity`) — *needs live API wiring*
- [x] Build Responder Dashboard UI (`ResponderDashboardActivity`, `ResponderHomeFragment`, `ResponderAssignedFragment`, `ResponderHistoryFragment`, `ResponderAlertsFragment`)
- [x] Connect `ResponderHomeFragment` / `ResponderAssignedFragment` to `GET /api/responder/dispatches`
- [x] Wire up Responder status progression UI (`ACCEPTED`, `DECLINED` with reason, `EN_ROUTE`/`RESPONDING`, `ON_SCENE`, `RESOLVED`) to `POST /api/responder/status`
- [x] Implement real-time notifications (`ArmsNotificationHelper` + 15s responder dispatch polling) for dispatches and status updates
- [ ] Test the complete dispatch → accept/decline → responding → on-scene → resolved workflow across devices

---

## PHASE 8 — Super Admin
- [x] Build Admin Dashboard UI (`AdminDashboardActivity`, `AdminHomeFragment`, `AdminUsersFragment`, `AdminTeamsFragment`, `AdminActivityFragment`, `AdminProfileFragment`)
- [x] Build Create Responder screen (`AdminCreateResponderActivity`) and backend staff provisioning (`POST /api/admin/provision-staff`)
- [x] Connect Admin UI (`AdminUsersFragment`, `AdminCreateResponderActivity`, `AdminActivityFragment`, `AdminIncidentRecordsActivity`) to live `/api/admin/*` endpoints
- [x] Implement Incident Records & Analytics screens (`AdminIncidentRecordsActivity` + `GET /api/admin/incidents`)
- [x] Implement Incident Category Management & Configurable Routing Rules UI (`AdminCategoriesRoutingActivity`)
- [x] Implement Audit Logs viewer connected to `audit_logs` table (`AdminActivityFragment` → `GET /api/admin/audit-logs`)
- [ ] Implement permitted System Settings

---

## PHASE 9 — Integration and Testing
- [ ] Test all user roles (Guest, Citizen, Dispatcher, Responder, Super Admin)
- [ ] Test authorization and report ownership enforcement on backend
- [ ] Test complete end-to-end incident workflow from submission to resolution across separate devices
- [ ] Test camera, audio, and location permission denials & GPS unavailability
- [ ] Test media upload failures, duplicate submissions, and network interruptions
- [ ] Test assignment, decline, and reassignment workflows
- [ ] Test database constraints and status transition validation
- [ ] Resolve all build and runtime errors

---

## PHASE 10 — Deployment Preparation
- [ ] Review security and privacy (remove hardcoded test/bypass logic, secure JWT & secrets)
- [ ] Configure production Node.js backend and PostgreSQL database
- [ ] Verify database backups and recovery
- [ ] Configure HTTPS / secure production API communication (disable cleartext traffic for production)
- [ ] Review media retention and access controls
- [ ] Validate responder routing with Valencia City authorities (CDRRMO, BFP, PNP, Medical/CHO)
- [x] Prepare deployment and maintenance documentation (`DEPLOYMENT_GUIDE.md` + `backend/test_api_smoke.js`)

---

## ACCEPTANCE CRITERIA (Section 18 Summary)
- [ ] Android app builds and launches cleanly
- [x] Welcome and access selection work (`WelcomeActivity`)
- [x] Guest reporting UI works without registration (`GuestReportActivity` → `IncidentLocationActivity`)
- [x] New incident media is captured inside the app (no gallery upload for new evidence)
- [x] Incident type, GPS coordinates, and timestamps are captured
- [ ] Reports are reliably saved through Node.js API to PostgreSQL with media
- [ ] Authorized dispatchers can review and assign incidents
- [ ] Responders can receive, accept/decline, and update incident status via backend
- [ ] Citizens and guests can track only their own reports from backend
- [ ] Guest-to-account linking verifies ownership
- [ ] Public registration cannot create privileged accounts (enforced end-to-end)
- [x] Database credentials are not exposed in the Android app
- [ ] Complete incident workflow tested end-to-end across all roles
