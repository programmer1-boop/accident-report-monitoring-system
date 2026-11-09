# ARMS MASTER PLAN
## Accident Reports and Monitoring System

**Project Location:** Valencia City, Bukidnon, Philippines  
**Application Platform:** Android Mobile Application  
**IDE:** Android Studio  
**Frontend Language:** Java  
**UI Technology:** XML Layouts  
**Backend Runtime:** Node.js  
**Database:** PostgreSQL  
**API Architecture:** REST API  
**Backend Framework:** Inspect the existing project first. If no framework exists, propose Express.js for approval.  
**Primary Goal:** Emergency incident reporting, dispatching, responder coordination, and report monitoring.

---

# 1. PROJECT PURPOSE

The Accident Reports and Monitoring System (ARMS) is a mobile application designed to help people report accidents and emergencies in Valencia City, Bukidnon.

The application must allow guests and registered citizens to submit incident reports using newly captured photos or videos, manually selected incident types, GPS coordinates, timestamps, and optional incident descriptions.

Submitted reports must be stored in PostgreSQL through a Node.js backend API. Authorized dispatchers must be able to review incidents and assign appropriate responder teams.

Responders must be able to accept assignments and update incident progress. The original reporter must be able to monitor the status of their own report.

ARMS must function as one integrated system. Its Android screens, Node.js API, PostgreSQL database, media storage, authentication, authorization, and incident workflows must work together.

This is an emergency reporting and monitoring system, not an AI incident-classification application.

# 2. CONFIRMED TECHNOLOGY STACK

## Android Frontend

- Android Studio
- Java
- XML layouts
- Android camera and location APIs
- HTTP communication with the backend REST API

## Backend

- Node.js
- REST API architecture
- Existing Node.js framework, if already configured
- Express.js may be proposed if no backend framework exists, but inspect the project before adding it.
- Backend authentication, authorization, validation, incident processing, responder assignment, and database access

## Database

- PostgreSQL
- Relational tables with primary keys and foreign keys
- Database constraints, indexes, and transactions
- Version-controlled schema migrations
- Parameterized SQL queries or an equivalent safe database access layer

## Architecture

Android Application (Java/XML)
→ Node.js REST API
→ PostgreSQL Database

Media uploads must use an appropriately protected storage solution. PostgreSQL should normally store media metadata and storage references instead of large media files.

Do not introduce Supabase-specific services or SDKs.

Do not replace PostgreSQL with another database.

Do not connect the Android application directly to PostgreSQL.

---

# 3. CORE DEVELOPMENT RULES

The AI coding agent must follow these rules throughout development.

1. Read this entire master plan before changing code.
2. Inspect the existing Android project, Node.js backend, and PostgreSQL configuration.
3. Preserve working code and existing project configurations whenever possible.
4. Identify existing features before creating replacements.
5. Use Java for Android application logic and XML for Android layouts.
6. Use Node.js for backend logic.
7. Use PostgreSQL as the persistent database.
8. Follow the existing backend framework if one is already configured.
9. Do not invent a framework or add unnecessary dependencies without justification.
10. Never place PostgreSQL credentials, database passwords, or privileged backend secrets inside the Android application.
11. Store backend secrets in environment variables or an appropriate secrets-management system.
12. Use consistent naming conventions for API routes, database fields, Java models, and backend models.
13. Separate UI, business logic, repositories, API clients, data models, and utility functions.
14. Enforce permissions on the backend, not only through the Android UI.
15. Do not create disconnected screens or nonfunctional buttons.
16. Do not implement AI-based incident classification.
17. Require reporters to select incident types manually.
18. Do not permit gallery uploads as evidence for new reports.
19. Do not permit public responder registration.
20. Do not falsely report a successful submission, assignment, or resolution.
21. Build and test the project after each meaningful implementation task.
22. Keep documentation updated when the approved architecture or database schema changes.
23. Do not implement all features in a single coding pass.
24. Use mock data only when explicitly labeled as temporary.
25. Ask for clarification when an unresolved decision affects security, architecture, or emergency operations.
26. Never delete or overwrite important existing project files without first understanding their purpose.
27. Keep all status values and field names consistent across the frontend, backend, and database.
28. Document incomplete features honestly instead of claiming they are functional.

---

# 4. USER ROLES

## 4.1 Guest Reporter

Guests can submit emergency reports without registration.

Allowed functions:

- Open guest reporting from the welcome screen.
- Capture a new photo inside the application.
- Record a new video inside the application.
- Select an incident type.
- Enter optional incident details.
- Obtain current GPS coordinates and location accuracy when available.
- Record relevant timestamps.
- Review evidence and report details before submission.
- Submit the report.
- Receive an incident reference and secure tracking access.
- Monitor only their own report.

Restrictions:

- No gallery attachment for new incident evidence.
- No access to the complete citizen dashboard.
- No access to other people's reports.
- No access to dispatcher, responder, or Super Admin features.
- No access to guest reports belonging to other people through guessed incident IDs or references.

If the guest later registers or signs in, the system should support linking their previous guest report to the authenticated account after ownership verification.

## 4.2 Citizen

Allowed functions:

- Register and sign in.
- Submit emergency reports.
- Capture new incident photos and videos inside the app.
- View personal reports.
- Monitor report status.
- View permitted incident details and response updates.
- Manage permitted profile settings.
- Sign out securely.

Restrictions:

- Cannot view or modify another citizen's private reports.
- Cannot assign responders.
- Cannot directly approve or resolve reports without authorization.
- Cannot grant privileged roles to themselves.

## 4.3 Dispatcher

Allowed functions:

- Sign in through an authorized dispatcher account.
- View the incident queue.
- Review submitted incidents.
- Verify reports.
- Reject invalid reports with a recorded reason.
- View incident details, evidence, location, and timestamps within their authorized scope.
- Assign appropriate responder teams.
- Reassign incidents when necessary.
- Monitor incident progress.

Restrictions:

- Cannot grant Super Admin privileges.
- Cannot bypass backend authorization.
- Cannot access functions outside their assigned role.

## 4.4 Responder

Responder accounts must be created and managed by an authorized Super Admin.

There must be no public responder registration.

Allowed functions:

- Sign in using an authorized responder account.
- View incidents assigned to their team or authorized scope.
- Accept or decline assignments.
- Provide a decline reason when appropriate.
- View necessary incident information and location.
- Update response progress.
- Mark the incident as on scene.
- Mark an incident resolved when authorized by the workflow.

Restrictions:

- Cannot create privileged accounts.
- Cannot modify system-level settings.
- Cannot access unrelated incidents outside their authorized scope.

## 4.5 Super Admin

Allowed functions:

- Manage authorized accounts and roles.
- Create, activate, suspend, and deactivate responder accounts.
- Manage responder teams.
- Manage incident categories.
- Manage approved routing rules.
- View authorized audit logs.
- Manage permitted system settings.

Super Admin privileges must be granted through a trusted administrative process. Public registration must never grant administrative access.

---

# 5. APPLICATION ENTRY AND NAVIGATION FLOW

## 5.1 Welcome Flow

Splash Screen
→ Welcome / Access Selection Screen

The access-selection screen must prominently provide:

- Sign in as Guest
- Sign In
- Sign Up

Guest emergency reporting must be easy to find.

Sign Up must be intended for citizen accounts only.

## 5.2 Guest Reporting Flow

Welcome Screen
→ Sign in as Guest
→ Guest Reporting
→ Capture Photo or Video
→ Select Incident Type
→ Confirm GPS Location
→ Enter Optional Details
→ Review Report
→ Submit Report
→ Submission Confirmation
→ Secure Guest Report Tracking
→ Monitor Status

## 5.3 Citizen Flow

Welcome Screen
→ Sign Up or Sign In
→ Authentication
→ Role Validation
→ Citizen Dashboard
→ Report Incident / My Reports / Profile

## 5.4 Dispatcher Flow

Authorized Sign In
→ Dispatcher Dashboard
→ Incident Queue
→ Review Incident
→ Verify or Reject
→ Assign Responder Team
→ Monitor Incident Progress

## 5.5 Responder Flow

Authorized Sign In
→ Responder Dashboard
→ Assigned Incidents
→ Accept or Decline Assignment
→ Responding
→ On Scene
→ Resolve Incident, when authorized

## 5.6 Super Admin Flow

Authorized Sign In
→ Super Admin Dashboard
→ Account Management
→ Responder Team Management
→ Incident Category Management
→ Audit Logs
→ System Settings

All protected navigation must validate authorization. Hiding a button or screen is not sufficient to protect backend data.

---

# 6. EMERGENCY REPORTING WORKFLOW

The reporter must be able to complete the following process:

1. Open the reporting screen.
2. Grant camera and location permissions when required.
3. Capture a new photo or record a new video inside the application.
4. Select the incident type manually.
5. Retrieve the current device location when available.
6. Record capture time and relevant location accuracy information.
7. Enter an optional description.
8. Review the captured evidence and incident details.
9. Validate the required fields.
10. Submit the report to the Node.js REST API.
11. Upload the media using the approved protected storage workflow.
12. Save the incident record and related metadata in PostgreSQL.
13. Generate a unique incident reference.
14. Make the incident available to authorized dispatchers.
15. Show a confirmation and provide secure report tracking.

Do not allow the gallery to be used for new incident evidence.

If camera access, GPS retrieval, media upload, or database operations fail, display an appropriate error and provide a safe retry mechanism.

Never fabricate GPS coordinates or show false submission success.

Use idempotency or an equivalent mechanism to reduce duplicate reports caused by repeated submissions and network retries.

The system must distinguish a locally captured report from a report successfully accepted by the backend.

---

# 7. INCIDENT CATEGORIES AND RESPONDER ROUTING

Initial incident categories:

- Road Accident
- Fire
- Medical Emergency
- Crime or Security Incident
- Disaster or Rescue
- Other Emergency

Proposed responder teams:

### Valencia City CDRRMO

Disaster coordination, rescue, and relevant emergency incidents.

### Bureau of Fire Protection (BFP)

Fire incidents and appropriate fire-rescue operations.

### Philippine National Police (PNP)

Crime, security, traffic-related incidents, and appropriate road-accident support.

### Valencia City Health Office / Authorized Medical Response Team

Medical emergencies and authorized medical response.

These are proposed routing guidelines and must be validated with the relevant local authorities before operational deployment.

Routing rules must be configurable by authorized administrators.

Dispatchers must be able to assign appropriate responder teams based on incident type, location, availability, and approved operating procedures.

An incident may require multiple teams when coordinated assistance is necessary.

The reporter manually selects the incident type. AI-based incident classification is not part of the project.

If no suitable responder is available, keep the incident visible in the dispatch queue and clearly show that it remains unassigned. Do not falsely mark it as accepted or responding.

The application must not treat offline responders as available for urgent assignments.

---

# 8. INCIDENT STATUS LIFECYCLE

Use consistent status values across the Android application, Node.js API, and PostgreSQL database.

Initial status values:

- SUBMITTED
- UNDER_REVIEW
- VERIFIED
- REJECTED
- ASSIGNED
- ACCEPTED
- RESPONDING
- ON_SCENE
- RESOLVED
- CANCELLED, when permitted

Primary workflow:

SUBMITTED
→ UNDER_REVIEW
→ VERIFIED
→ ASSIGNED
→ ACCEPTED
→ RESPONDING
→ ON_SCENE
→ RESOLVED

Alternative transitions:

- UNDER_REVIEW → REJECTED
- ASSIGNED → reassignment when declined or unavailable
- Eligible active states → CANCELLED under an authorized cancellation policy

The backend must validate every status transition.

Every status change must record:

- Incident ID
- Previous status
- New status
- Actor ID, where applicable
- Actor role or action source
- Timestamp
- Reason or notes, where applicable

Citizens and guests must not directly set protected statuses such as VERIFIED, ACCEPTED, or RESOLVED.

Assignment history must be retained when incidents are reassigned.

The tracking screen must obtain status information from the backend instead of relying on hardcoded sample values.

---

# 9. POSTGRESQL DATABASE DESIGN

PostgreSQL is the primary persistent database.

Before creating or modifying tables, inspect the existing schema and project configuration.

Create version-controlled migration scripts and document all relationships, constraints, indexes, and access rules.

Do not recreate tables that already exist without inspecting their purpose and current data.

## 9.1 profiles

Purpose: Store application user profile information.

Suggested fields:

- id: UUID
- full_name
- phone_number, if required
- role
- account_status
- created_at
- updated_at

Allowed roles:

- CITIZEN
- DISPATCHER
- RESPONDER
- SUPER_ADMIN

The backend must control role assignment and changes.

The profile ID should be consistently associated with the authenticated identity.

## 9.2 responder_teams

Purpose: Store responder organizations and teams.

Suggested fields:

- id
- team_name
- team_type
- description
- is_active
- created_at
- updated_at

## 9.3 responder_memberships

Purpose: Associate authorized responders with teams.

Suggested fields:

- id
- responder_profile_id
- responder_team_id
- is_active
- created_at

## 9.4 incidents

Purpose: Store incident reports.

Suggested fields:

- id: UUID
- incident_reference: unique human-readable reference
- reporter_user_id: nullable for guest reports
- guest_tracking_record_id or equivalent secure guest ownership reference
- incident_type
- description
- latitude
- longitude
- location_accuracy
- captured_at
- submitted_at
- current_status
- created_at
- updated_at

The database must distinguish the capture timestamp from the server submission timestamp.

Location values must support unavailable GPS information according to the approved reporting policy.

## 9.5 incident_media

Purpose: Store evidence metadata and protected media references.

Suggested fields:

- id
- incident_id
- storage_path
- media_type
- captured_at
- uploaded_at
- upload_status

Store media files in an appropriate protected storage solution. PostgreSQL should normally store metadata and storage references, not large media files.

## 9.6 incident_assignments

Purpose: Store assignments and reassignment history.

Suggested fields:

- id
- incident_id
- responder_team_id
- assigned_by
- assigned_at
- accepted_at
- completed_at
- assignment_status
- decline_reason

Retain historical assignment records.

## 9.7 incident_status_history

Purpose: Store status transitions.

Suggested fields:

- id
- incident_id
- previous_status
- new_status
- changed_by
- change_reason
- created_at

## 9.8 guest_report_access

Purpose: Support secure guest report ownership.

Suggested fields:

- id
- incident_id
- protected_token_hash or equivalent secure credential representation
- created_at
- expires_at, if applicable
- revoked_at, if applicable

Generate unpredictable access credentials. Never use a public incident reference alone as proof of ownership.

## 9.9 device_registrations

Purpose: Support the intended one-account-per-device restriction.

Suggested fields:

- id
- profile_id
- device_installation_id or equivalent installation identifier
- registered_at
- revoked_at
- is_active

The implementation must consider reinstallations, device resets, shared devices, device replacement, and account recovery.

The policy must not prevent emergency guest reporting.

## 9.10 audit_logs

Purpose: Record relevant administrative and operational actions.

Suggested fields:

- id
- actor_profile_id
- action
- entity_type
- entity_id
- created_at
- relevant_metadata

Restrict audit log access to authorized users.

## 9.11 DATABASE RULES

- Use primary keys and foreign keys where appropriate.
- Add suitable NOT NULL and unique constraints.
- Add indexes for frequently queried fields.
- Use transactions for operations that must succeed or fail together.
- Use parameterized SQL queries or an equivalent safe data-access layer.
- Keep schema migrations version-controlled.
- Define appropriate data-retention rules.
- Avoid storing unnecessary sensitive personal information.
- Use consistent timestamps and data types.
- Document relationships and constraints.
- Review the actual database before applying schema changes.

This schema is a blueprint. The coding agent must adapt it to the existing project and approved requirements.

---

# 10. NODE.JS BACKEND ARCHITECTURE

Node.js is the confirmed backend runtime.

The backend must expose a REST API used by the Android application.

## 10.1 Backend Responsibilities

The Node.js backend must handle:

- Authentication and session validation.
- Authorization and role verification.
- PostgreSQL queries and transactions.
- Incident creation and validation.
- Guest report ownership verification.
- Citizen report retrieval.
- Secure guest-to-account linking.
- Media upload coordination.
- Incident review and verification.
- Responder team assignment.
- Incident status transitions.
- Audit logging.
- Input validation and error handling.

## 10.2 Framework Selection

First inspect the existing Node.js project.

If Express.js or another framework is already configured, preserve and extend the existing architecture where appropriate.

If no framework exists, propose Express.js for approval before adding it.

Do not create a second backend server if a functioning backend already exists.

## 10.3 Backend Project Structure

A possible structure for a new Node.js backend is:

```text
backend/
├── src/
│   ├── config/
│   ├── routes/
│   ├── controllers/
│   ├── services/
│   ├── repositories/
│   ├── middleware/
│   ├── validators/
│   ├── models/
│   ├── utils/
│   └── app.js
├── migrations/
├── tests/
├── .env.example
├── .gitignore
└── package.json
```

This is a proposed structure, not a requirement to duplicate existing folders.

## 10.4 PostgreSQL Connection

- Use an appropriate PostgreSQL driver or database library in Node.js.
- Store database connection settings in backend environment variables.
- Use connection pooling where appropriate.
- Use parameterized queries.
- Handle database connection errors and transaction failures.
- Use migrations for schema changes.
- Do not expose database credentials through API responses or logs.
- Use secure connections in production.
- Do not place privileged credentials inside Android resources or source files.

## 10.5 REST API Design

Design consistent API endpoints for:

- Authentication
- Citizen profiles
- Incident creation
- Incident media
- Guest report tracking
- Citizen report lists
- Dispatcher incident queues
- Incident verification and rejection
- Responder assignments
- Responder status updates
- Super Admin account management
- Responder team management
- Audit logs

Confirm endpoint names and request/response formats before implementation.

All protected API endpoints must validate authentication, permissions, input, and incident ownership.

---

# 11. AUTHENTICATION AND ACCESS CONTROL

Citizen authentication must support registration, sign-in, sign-out, and appropriate session handling.

The authentication method must be selected after inspecting the existing backend and project requirements.

The backend must verify identities and permissions for protected requests.

Public registration must create citizen accounts only.

Responder, dispatcher, and Super Admin roles must be provisioned through trusted administrative processes.

The backend must check account status before allowing protected actions.

Never rely solely on client-side role values or hidden UI elements.

Use secure password handling if the project implements password-based authentication. Never store plaintext passwords.

Use secure session or token handling and appropriate expiration and revocation behavior.

Do not invent a complete authentication implementation before confirming the existing backend setup.

---

# 12. GUEST OWNERSHIP AND ACCOUNT LINKING

Guest reports must work without requiring registration.

Each guest report must use a secure ownership mechanism independent of the public incident reference.

Requirements:

1. Generate an unpredictable guest access credential or equivalent secure mechanism.
2. Protect credentials from exposure in logs and analytics.
3. Verify guest ownership for every tracking request.
4. Prevent access to another guest's report.
5. Support secure linking to an authenticated citizen account.
6. Verify ownership before linking.
7. Preserve incident details and status history.
8. Prevent duplicate or unauthorized linking.
9. Document credential expiration, revocation, and recovery behavior.

Do not use a guessable report number or database ID as the only access control.

---

# 13. CAMERA, MEDIA STORAGE, GPS, AND TIMESTAMPS

## 13.1 Camera and Media

- Capture new photos and videos inside the Android application.
- Do not offer gallery attachment for new incident evidence.
- Validate media type and file size.
- Upload evidence using the approved backend workflow.
- Store media metadata in PostgreSQL.
- Use protected media storage and controlled access.
- Handle upload failures and retries.
- Prevent incomplete uploads from being treated as fully submitted reports.

## 13.2 GPS

- Request location permission when required.
- Obtain current device coordinates and accuracy when available.
- Handle denied permissions and unavailable GPS.
- Allow the reporter to review the reported location.
- Never fabricate location data.
- Use a suitable map provider and verify licensing, usage limits, and costs.

## 13.3 Timestamps

Distinguish between:

- Media capture time
- Incident submission time
- Server receipt time
- Status update time

Use a trusted server-side timestamp for server receipt and status transitions.

Document how device time and time-zone differences are handled.

---

# 14. UI AND DESIGN REQUIREMENTS

Use a modern, professional emergency-oriented design.

Primary palette:

- Navy Blue
- Emergency Red
- White
- Light Gray

Requirements:

- Clear typography.
- Consistent spacing and reusable components.
- Consistent buttons, cards, navigation, and status indicators.
- Prominent emergency reporting action.
- Easy-to-find camera action.
- Clear loading, empty, error, and success states.
- Accessible touch targets and sufficient contrast.
- Responsive layouts for supported Android screen sizes.
- Consistent ARMS logo and branding.
- Clear confirmation for important actions.

Create reusable XML resources for colors, strings, dimensions, themes, and styles.

Do not implement only static designs while claiming the system is functional.

---

# 15. SCREEN INVENTORY

The following are logical screen responsibilities. Reuse Activities, Fragments, and components where appropriate.

## Public and Authentication

- SplashActivity
- WelcomeActivity
- AccessSelectionActivity
- LoginActivity
- CitizenRegistrationActivity

## Guest

- GuestReportingActivity
- GuestCaptureActivity, if needed
- IncidentTypeSelectionActivity, if needed
- ReportReviewActivity
- GuestReportTrackingActivity

## Citizen

- CitizenDashboardActivity
- CitizenReportActivity
- MyReportsActivity
- CitizenReportDetailsActivity
- CitizenProfileActivity
- CitizenSettingsActivity

## Dispatcher

- DispatcherDashboardActivity
- IncidentQueueActivity
- IncidentReviewActivity
- IncidentAssignmentActivity
- DispatcherIncidentDetailsActivity

## Responder

- ResponderDashboardActivity
- AssignedIncidentsActivity
- ResponderIncidentDetailsActivity
- ResponseStatusUpdateActivity

## Super Admin

- AdminDashboardActivity
- AccountManagementActivity
- ResponderTeamManagementActivity
- IncidentCategoryManagementActivity
- AuditLogActivity
- SystemSettingsActivity

Inspect the existing Android project before creating these screens. Reuse working components and avoid unnecessary duplication.

---

# 16. ANDROID APPLICATION ARCHITECTURE

Suggested package organization:

```text
com.example.arms/
├── data/
│   ├── model/
│   ├── repository/
│   └── remote/
├── ui/
│   ├── welcome/
│   ├── auth/
│   ├── guest/
│   ├── citizen/
│   ├── dispatcher/
│   ├── responder/
│   └── admin/
├── navigation/
├── security/
├── location/
├── media/
├── notifications/
└── utils/
```

Adapt the package name to the actual project.

Responsibilities:

- Models represent incident, profile, assignment, and response data.
- Repositories coordinate application data access.
- API clients communicate with the Node.js REST API.
- UI components manage presentation and user interactions.
- Location utilities retrieve device coordinates.
- Media utilities support in-app capture and upload.
- Navigation components manage screen transitions.
- Security utilities handle appropriate client-side session behavior.

Keep network requests off the main Android UI thread.

Use consistent API response models and error handling.

Do not expose backend secrets in Android code.

---

# 17. DEVELOPMENT PHASES

## PHASE 1 — Master Plan and Project Inspection

- Finalize this master plan.
- Inspect the Android project.
- Inspect the Node.js backend.
- Inspect PostgreSQL configuration and existing tables.
- Confirm the existing framework and dependencies.
- Identify existing and missing components.
- Propose the exact implementation order.
- Document architectural decisions and unresolved questions.

Do not implement the entire system in this phase.

## PHASE 2 — Project Foundation

- Configure the Android project.
- Confirm Java and Gradle configuration.
- Organize package structure.
- Configure XML themes and reusable resources.
- Establish screen navigation.
- Inspect and organize the Node.js backend.
- Prepare secure backend configuration.
- Verify that the Android application and backend can start successfully.

## PHASE 3 — UI and Navigation

- Build the approved screens.
- Connect navigation.
- Implement loading, error, empty, and success states.
- Connect buttons to intended actions.
- Use temporary data only when explicitly identified.
- Test the full screen-navigation flow.

## PHASE 4 — Node.js API and PostgreSQL

- Confirm the backend framework.
- Prepare the PostgreSQL schema and migrations.
- Configure secure database connectivity.
- Implement repositories and API endpoints.
- Implement authentication and authorization.
- Implement validation and consistent error responses.
- Test API and database operations.
- Configure appropriate media storage.

## PHASE 5 — Guest Emergency Reporting

- Implement in-app photo capture and video recording.
- Implement incident type selection.
- Implement GPS and timestamp handling.
- Implement secure media upload.
- Submit reports through the Node.js API.
- Save incidents and related records in PostgreSQL.
- Implement secure guest tracking.
- Verify that dispatchers can retrieve submitted reports.

## PHASE 6 — Citizen Features

- Implement citizen registration and sign-in.
- Implement the citizen dashboard.
- Implement My Reports and incident details.
- Implement status tracking.
- Implement secure guest-to-account linking.
- Implement the approved device restriction.
- Test access permissions.

## PHASE 7 — Dispatcher and Responder Features

- Implement report review and verification.
- Implement rejection and reason recording.
- Implement responder-team assignment.
- Implement acceptance and decline workflows.
- Implement response status updates.
- Implement reassignment.
- Implement incident resolution.
- Implement notifications where appropriate.
- Test the complete dispatch workflow.

## PHASE 8 — Super Admin

- Implement protected account management.
- Implement responder-team management.
- Implement incident category management.
- Implement approved routing rules.
- Implement audit logs.
- Implement permitted system settings.

## PHASE 9 — Integration and Testing

- Test all user roles.
- Test authorization and report ownership.
- Test the complete workflow from submission to resolution.
- Test camera and location permission errors.
- Test GPS unavailability.
- Test media upload failures.
- Test duplicate submissions and network interruptions.
- Test assignment and reassignment.
- Test database constraints and status transitions.
- Test guest tracking and account linking.
- Test Android and Node.js integration.
- Resolve build and runtime errors.

## PHASE 10 — Deployment Preparation

- Review security and privacy.
- Configure production backend and PostgreSQL.
- Verify database backups and recovery.
- Configure secure production API communication.
- Review media retention and access controls.
- Validate responder routing with relevant local authorities.
- Prepare deployment and maintenance documentation.

---

# 18. ACCEPTANCE CRITERIA

ARMS must not be considered complete until the following are verified:

1. The Android app builds and launches successfully.
2. Welcome and access selection work.
3. Guest reporting works without registration.
4. New incident media is captured inside the app.
5. Gallery attachment is not available for new incident evidence.
6. Incident type, GPS coordinates, and timestamps are handled correctly.
7. Reports are saved through the Node.js API to PostgreSQL.
8. Authorized dispatchers can review incidents.
9. Appropriate responders can receive and accept assignments.
10. Responders can update incident status within their permissions.
11. Citizens and guests can track only their own reports.
12. Guest-to-account linking verifies ownership.
13. Public registration cannot create privileged accounts.
14. Database credentials are not exposed in the Android app.
15. Network, media, and location errors are handled appropriately.
16. Database fields, backend models, API payloads, and Android models remain consistent.
17. Status history and assignment history are preserved.
18. The complete incident workflow is tested end to end.

---

# 19. FIRST TASK FOR THE AI CODING AGENT

Begin with inspection and planning only.

### Step 1 — Read the Master Plan

Read this entire ARMS_MASTER_PLAN.md file before changing any code.

### Step 2 — Inspect the Existing Project

Identify:

- Android package name
- Java version
- Gradle configuration
- Existing Activities and XML layouts
- Existing Android architecture
- Node.js version and package.json
- Existing backend framework and routes
- Existing backend folder structure
- PostgreSQL connection configuration
- Existing database schema and migration files
- Authentication implementation
- Media storage configuration
- Existing dependencies
- Current build errors and runtime issues

### Step 3 — Identify Existing Components

Classify each requirement as:

- Already implemented and working
- Partially implemented
- Not implemented
- Requires clarification

### Step 4 — Propose the Implementation Plan

Provide:

- Proposed project file structure
- Android navigation architecture
- Node.js backend architecture
- PostgreSQL schema and relationships
- Proposed REST API endpoints
- Authentication and authorization approach
- Media storage approach
- Implementation order
- Testing plan

### Step 5 — Identify Unresolved Decisions

Do not silently assume missing details that could affect security, architecture, or database integrity.

If the backend framework is missing, propose Express.js for approval.

If an existing authentication or media storage approach is missing, identify the options and ask for a decision when necessary.

### Step 6 — Wait Before Major Changes

Report the inspection findings and implementation plan first.

Do not rewrite the project, delete existing code, create a competing backend, or implement every feature at once.

After the inspection has been reviewed, proceed to Phase 2 and implement only the approved foundation.

---

# 20. FINAL PROJECT PRINCIPLE

ARMS must be developed as one integrated emergency reporting and monitoring system.

The Android application must communicate with the Node.js backend through secure API endpoints. The backend must validate requests and enforce authorization. PostgreSQL must store persistent incident, account, assignment, and status information.

Every module must use consistent data models and validated workflows.

No user may access information beyond their authorized permissions. No incident may be marked submitted, assigned, accepted, or resolved unless the corresponding operation has actually succeeded.

Build incrementally, test every phase, preserve working code, document changes, and ensure that all modules remain connected.
