# ARMS Production Deployment & Security Guide (Phase 10)
**Project:** Accident Reports and Monitoring System (Valencia City, Bukidnon, Philippines)

---

## 1. Environment & Secrets Security
- Replace `JWT_SECRET` in `backend/.env` with a strong random 64-character secret (`NODE_ENV=production`).
- Rotate default seeded passwords (`password123`) before going live.
- Remove offline/demo login shortcuts in `CitizenLoginActivity.java` before publishing a production APK/AAB.

## 2. HTTPS & Android Network Security
- Deploy the Node.js API behind an HTTPS reverse proxy (Nginx or Caddy with TLS/Let's Encrypt).
- Update `ApiClient.BASE_URL` in `app/src/main/java/.../remote/ApiClient.java` from `http://10.0.2.2:5000/api` to `https://your-domain.gov.ph/api`.
- Set `android:usesCleartextTraffic="false"` in `app/src/main/AndroidManifest.xml` for release builds.

## 3. PostgreSQL Backups & Recovery
- **Daily Backup (`pg_dump`):**
  ```powershell
  pg_dump -U postgres -d arms_db -F c -f arms_db_backup.dump
  ```
- **Restore (`pg_restore`):**
  ```powershell
  pg_restore -U postgres -d arms_db -c arms_db_backup.dump
  ```

## 4. Media Retention & Access Controls
- Uploaded incident evidence images are stored in `backend/uploads/`.
- Configure a retention/archival policy (e.g. archive resolved incident media older than 90 days) and restrict `/uploads` access to authenticated staff or signed URLs in production.

## 5. Valencia City Authority Routing Validation
Confirm responder routing rules and station coordinates with local agencies prior to deployment:
- **CDRRMO:** Valencia City Disaster Risk Reduction and Management Office
- **BFP:** Bureau of Fire Protection — Valencia Station
- **PNP:** Philippine National Police — Valencia Station
- **MEDICAL_EMS:** Valencia City Health Office / Valencia City Hospital Medical Unit
