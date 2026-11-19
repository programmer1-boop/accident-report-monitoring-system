# ARMS PHASE 1 — IMPLEMENTATION PLAN & ARCHITECTURAL DECISIONS
**Project:** Accident Reports and Monitoring System (Valencia City, Bukidnon)  
**References:** `ARMS_MASTER_PLAN.md` · `ARMS_CHECKLIST.md`

---

## 1. Confirmed Project Baseline
- **Android Frontend (`app/`):** Java 11, XML layouts (Material 3), `minSdk 23`, `targetSdk 37`, CameraX `1.3.1`, Leaflet.js via `WebView`, `LocationManager` GPS, `HttpURLConnection` (`ApiClient.java`), `SharedPreferences` (`SessionManager.java`).
- **Node.js Backend (`backend/`):** Express `4.18.2`, `pg` `8.11.3`, `bcryptjs` `2.4.3`, `jsonwebtoken` `9.0.2`, `cors`, `dotenv`.
- **PostgreSQL (`backend/migrations/001_initial_schema.sql`):** Relational schema with `profiles`, `user_credentials`, `responder_teams`, `responder_memberships`, `incidents`, `guest_report_access`, `incident_assignments`, `incident_status_history`, and `audit_logs`.

---

## 2. Exact Implementation Order

### Step 1 — Phase 2: Environment & Foundation Verification
1. Configure `backend/.env` from `backend/.env.example` and verify PostgreSQL connection (`arms_db`) + run `001_initial_schema.sql`.
2. Start `backend/src/server.js` and verify `/api/health` returns `database: CONNECTED` and default teams/accounts seed cleanly.
3. Verify Android project builds in Android Studio (`assembleDebug`) and can reach `http://10.0.2.2:5000/api/health` from the emulator.

### Step 2 — Phase 4: Backend Alignment & Bug Fixes
1. Fix `backend/src/routes/responder.js` line 19 (`t.agency_type` → `t.team_type` to match `001_initial_schema.sql`).
2. Standardize role names across Android & Backend (`SUPER_ADMIN` vs `ADMIN`, `DISPATCHER`, `RESPONDER`, `CITIZEN`).
3. Standardize incident status lifecycle values (`SUBMITTED`, `UNDER_REVIEW`, `VERIFIED`, `ASSIGNED`, `ACCEPTED`, `RESPONDING`, `ON_SCENE`, `RESOLVED`, `REJECTED`, `CANCELLED`).
4. Add multipart media upload support (e.g., `multer` + `incident_media` table / protected `/uploads` route) and password reset endpoint.

### Step 3 — Phase 6 & Phase 5: Real Authentication & Reporting Sync
1. Update `CitizenRegisterActivity.java` to collect/send `email` (matching `POST /api/auth/register`) and handle errors honestly.
2. Update `CitizenLoginActivity.java` to authenticate via `POST /api/auth/login` with real password verification and JWT persistence (keeping Demo buttons clearly separated or backed by seeded test accounts).
3. Fix `IncidentLocationActivity.java` logged-in citizen endpoint (`"/"` → `"/incidents"`), capture full-resolution media via `FileProvider`, and upload media + GPS to PostgreSQL.
4. Wire `GuestReportTrackingActivity` (`activity_guest_report_tracking.xml`) to `GET /api/incidents/guest/track` using `incident_reference` + `tracking_token`, and implement guest-to-citizen report linking.

### Step 4 — Phase 7: Dispatcher & Responder Workflows
1. Wire `DispatcherDashboardActivity` (`activity_dispatcher_dashboard.xml`) and `DispatcherIncidentReviewActivity` (`activity_dispatcher_incident_review.xml`) to `/api/dispatcher/*` (queue, verify, reject with reason, assign teams).
2. Wire `ResponderHomeFragment` / `ResponderAssignedFragment` to `GET /api/responder/dispatches` for each agency (`CDRRMO`, `BFP`, `PNP`, `MEDICAL_EMS`).
3. Wire `ResponderStatusUpdateActivity` (`activity_responder_status_update.xml`) to `POST /api/responder/status` (`ACCEPTED`, `DECLINED` with reason, `EN_ROUTE`/`RESPONDING`, `ON_SCENE`, `RESOLVED`).
4. Add real-time notification/update mechanism (polling or FCM).

### Step 5 — Phase 8: Super Admin Management
1. Connect `AdminUsersFragment`, `AdminTeamsFragment`, and `AdminCreateResponderActivity` to live `/api/admin/*` endpoints.
2. Wire `AdminIncidentRecordsActivity` (`activity_admin_incident_records.xml`), `AdminCategoriesRoutingActivity` (`activity_admin_categories_routing.xml`), and Audit Logs viewer.

### Step 6 — Phase 9 & 10: Integration Testing & Deployment Prep
1. Test all 5 roles end-to-end across separate devices/emulators.
2. Remove any temporary local-only bypasses/in-memory stores (`ValenciaGlobalIncidentStore`, `ValenciaGlobalUserStore`), enforce HTTPS, and validate routing with Valencia City authorities.

---

## 3. Key Architectural Decisions
- **API Communication:** Keep `ApiClient.java` (`HttpURLConnection` + `ExecutorService` + `Handler`) rather than adding heavy networking libraries, or extend it to support `PUT`/`PATCH`/`DELETE` and `multipart/form-data` uploads.
- **Responder Agency Dashboards:** `BfpDashboardActivity`, `CdrrmoDashboardActivity`, `PnpDashboardActivity`, and `MedicalDashboardActivity` extend `ResponderDashboardActivity` and filter/display dispatches by the authenticated responder's team membership (`team_type`).
- **Security:** Never embed PostgreSQL credentials in `app/`. All authorization checks (role + ownership) are enforced in Express middleware (`authenticateToken`, `requireRole`) and parameterized SQL queries.

---

## 4. Unresolved Questions for Clarification
1. **Media Storage:** Should uploaded incident photos/videos be stored on the Node.js server filesystem (e.g., `backend/uploads/` served behind auth) for local/thesis deployment, or in an external object store (e.g., S3/MinIO)?
2. **Real-Time Updates:** Do you prefer lightweight HTTP polling (e.g., every 10–15 seconds on active dashboards) or Firebase Cloud Messaging (FCM) push notifications?
3. **Demo Login Buttons:** Should the **Quick Demo Login** buttons on `CitizenLoginActivity` authenticate against the seeded backend accounts (`password123` via `/api/auth/login`) once the database is connected, or remain local-only for offline UI demos?
4. **Testing Target:** Will you test primarily on the Android Studio Emulator (`10.0.2.2:5000`) or on physical Android phones over Wi-Fi (requiring your PC's LAN IP in `ApiClient.BASE_URL`)?
