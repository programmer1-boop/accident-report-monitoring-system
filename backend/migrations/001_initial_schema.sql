
-- ARMS: Accident Reports and Monitoring System
-- Initial PostgreSQL Schema Migration
-- Target: Valencia City, Bukidnon, Philippines
-- Status: PREPARED FOR REVIEW - NOT EXECUTED
--
-- Requirements:
--   PostgreSQL 13+ (gen_random_uuid() is built in)
--   No uuid-ossp extension required
--   pgcrypto is not required by this schema
--
-- Migration policy:
--   Apply only to a new development database after review.
--   CREATE TABLE IF NOT EXISTS does not upgrade existing tables.
--   Use a migration runner/version table to track applied migrations.

BEGIN;

-- =========================================================
-- 1. UPDATED_AT TRIGGER FUNCTION
-- =========================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

-- =========================================================
-- 2. PROFILES
-- =========================================================

CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone_number VARCHAR(20),
    role VARCHAR(50) NOT NULL
        CHECK (role IN (
            'CITIZEN', 'RESPONDER', 'DISPATCHER', 'SUPER_ADMIN'
        )),
    account_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
        CHECK (account_status IN (
            'ACTIVE', 'SUSPENDED', 'DEACTIVATED'
        )),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Case-insensitive uniqueness for non-null email addresses.
CREATE UNIQUE INDEX IF NOT EXISTS uq_profiles_email_lower
    ON profiles (LOWER(email))
    WHERE email IS NOT NULL;

CREATE TRIGGER trg_profiles_updated_at
BEFORE UPDATE ON profiles
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =========================================================
-- 3. USER CREDENTIALS
-- Store only a modern password hash, never a plaintext password.
-- Node.js should use Argon2id or an appropriately configured
-- bcrypt implementation and verify hashes using its library.
-- =========================================================

CREATE TABLE IF NOT EXISTS user_credentials (
    user_id UUID PRIMARY KEY
        REFERENCES profiles(id) ON DELETE RESTRICT,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_credentials_updated_at
BEFORE UPDATE ON user_credentials
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =========================================================
-- 4. RESPONDER TEAMS
-- Teams start inactive until an authorized administrator
-- confirms the participating agency and configures the team.
-- =========================================================

CREATE TABLE IF NOT EXISTS responder_teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_name VARCHAR(255) NOT NULL UNIQUE,
    team_type VARCHAR(100) NOT NULL
        CHECK (team_type IN (
            'CDRRMO', 'BFP', 'PNP', 'MEDICAL_EMS',
            'RED_CROSS', 'OTHER'
        )),
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_responder_teams_updated_at
BEFORE UPDATE ON responder_teams
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =========================================================
-- 5. RESPONDER MEMBERSHIPS
-- =========================================================

CREATE TABLE IF NOT EXISTS responder_memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    responder_profile_id UUID NOT NULL
        REFERENCES profiles(id) ON DELETE RESTRICT,
    responder_team_id UUID NOT NULL
        REFERENCES responder_teams(id) ON DELETE RESTRICT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_responder_team_membership
        UNIQUE (responder_profile_id, responder_team_id)
);

CREATE INDEX IF NOT EXISTS idx_memberships_team_active
    ON responder_memberships (responder_team_id, is_active);

-- =========================================================
-- 6. INCIDENTS
-- Device capture time is client-provided and must not be
-- treated as a trusted server timestamp. submitted_at and
-- created_at are server-generated defaults.
-- =========================================================

CREATE TABLE IF NOT EXISTS incidents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_reference VARCHAR(100) NOT NULL UNIQUE,
    reporter_user_id UUID
        REFERENCES profiles(id) ON DELETE SET NULL,

    incident_type VARCHAR(100) NOT NULL
        CHECK (incident_type IN (
            'ROAD_ACCIDENT', 'FIRE', 'MEDICAL_EMERGENCY',
            'CRIME_SECURITY', 'DISASTER_RESCUE',
            'OTHER_EMERGENCY'
        )),

    description TEXT,
    latitude NUMERIC(10, 7)
        CHECK (latitude BETWEEN -90 AND 90),
    longitude NUMERIC(10, 7)
        CHECK (longitude BETWEEN -180 AND 180),
    location_accuracy NUMERIC(8, 2)
        CHECK (location_accuracy >= 0),
    landmark_address TEXT,

    captured_at TIMESTAMPTZ,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    current_status VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED'
        CHECK (current_status IN (
            'SUBMITTED', 'UNDER_REVIEW', 'VERIFIED', 'REJECTED',
            'ASSIGNED', 'ACCEPTED', 'RESPONDING',
            'ON_SCENE', 'RESOLVED', 'CANCELLED'
        )),

    linked_by_user_id UUID
        REFERENCES profiles(id) ON DELETE SET NULL,
    linked_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_gps_both_or_none CHECK (
        (latitude IS NULL AND longitude IS NULL)
        OR (latitude IS NOT NULL AND longitude IS NOT NULL)
    ),

    CONSTRAINT chk_link_fields_consistent CHECK (
        (linked_by_user_id IS NULL AND linked_at IS NULL)
        OR (linked_by_user_id IS NOT NULL AND linked_at IS NOT NULL)
    )
);

CREATE TRIGGER trg_incidents_updated_at
BEFORE UPDATE ON incidents
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =========================================================
-- 7. GUEST REPORT ACCESS
-- Store a hash of a cryptographically random tracking token.
-- Never store or log the raw token.
-- =========================================================

CREATE TABLE IF NOT EXISTS guest_report_access (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL UNIQUE
        REFERENCES incidents(id) ON DELETE RESTRICT,
    protected_token_hash VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,

    CONSTRAINT chk_guest_token_expiry CHECK (
        expires_at IS NULL OR expires_at > created_at
    )
);

CREATE INDEX IF NOT EXISTS idx_guest_access_valid_token
    ON guest_report_access (protected_token_hash)
    WHERE revoked_at IS NULL;

-- Secure guest-to-citizen linking is a backend workflow:
-- 1. Authenticate the citizen account.
-- 2. Require proof of possession of the original guest token
--    or another explicitly approved ownership-verification method.
-- 3. Hash and verify the token server-side; check expiry/revocation.
-- 4. In one transaction, lock the incident and access record,
--    confirm reporter_user_id IS NULL, then set reporter_user_id,
--    linked_by_user_id, and linked_at.
-- 5. Record the linking event in audit_logs.
-- 6. Revoke guest tracking access only according to the approved
--    post-link policy. Never authorize linking by public incident ID.
-- The SQL schema alone does not implement this authorization workflow.

-- =========================================================
-- 8. INCIDENT MEDIA
-- storage_path should identify private storage, not a public URL.
-- Backend validates actual media contents and upload permissions.
-- =========================================================

CREATE TABLE IF NOT EXISTS incident_media (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL
        REFERENCES incidents(id) ON DELETE RESTRICT,
    storage_path TEXT NOT NULL,
    media_type VARCHAR(50) NOT NULL DEFAULT 'IMAGE'
        CHECK (media_type IN ('IMAGE', 'VIDEO')),
    captured_at TIMESTAMPTZ,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upload_status VARCHAR(50) NOT NULL DEFAULT 'PENDING'
        CHECK (upload_status IN (
            'PENDING', 'UPLOADING', 'COMPLETED', 'FAILED'
        ))
);

CREATE INDEX IF NOT EXISTS idx_incident_media_incident
    ON incident_media (incident_id);

-- =========================================================
-- 9. INCIDENT ASSIGNMENTS
-- Active statuses: DISPATCHED, ACCEPTED, EN_ROUTE, ON_SCENE.
-- At most one active assignment is allowed per incident.
-- A declined, resolved, or reassigned assignment is not active.
-- =========================================================

CREATE TABLE IF NOT EXISTS incident_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL
        REFERENCES incidents(id) ON DELETE RESTRICT,
    responder_team_id UUID NOT NULL
        REFERENCES responder_teams(id) ON DELETE RESTRICT,
    assigned_by UUID
        REFERENCES profiles(id) ON DELETE SET NULL,

    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at TIMESTAMPTZ,
    en_route_at TIMESTAMPTZ,
    on_scene_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,

    assignment_status VARCHAR(50) NOT NULL DEFAULT 'DISPATCHED'
        CHECK (assignment_status IN (
            'DISPATCHED', 'ACCEPTED', 'DECLINED',
            'EN_ROUTE', 'ON_SCENE', 'RESOLVED', 'REASSIGNED'
        )),

    decline_reason TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_one_active_assignment_per_incident
    ON incident_assignments (incident_id)
    WHERE assignment_status IN (
        'DISPATCHED', 'ACCEPTED', 'EN_ROUTE', 'ON_SCENE'
    );

CREATE INDEX IF NOT EXISTS idx_assignments_team_status
    ON incident_assignments (responder_team_id, assignment_status);

-- Backend must create/reassign assignments in a transaction and
-- handle unique-constraint conflicts. A partial unique index protects
-- the one-active-assignment rule even under concurrent requests.

-- =========================================================
-- 10. INCIDENT STATUS HISTORY
-- =========================================================

CREATE TABLE IF NOT EXISTS incident_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL
        REFERENCES incidents(id) ON DELETE RESTRICT,

    previous_status VARCHAR(50)
        CHECK (previous_status IS NULL OR previous_status IN (
            'SUBMITTED', 'UNDER_REVIEW', 'VERIFIED', 'REJECTED',
            'ASSIGNED', 'ACCEPTED', 'RESPONDING',
            'ON_SCENE', 'RESOLVED', 'CANCELLED'
        )),

    new_status VARCHAR(50) NOT NULL
        CHECK (new_status IN (
            'SUBMITTED', 'UNDER_REVIEW', 'VERIFIED', 'REJECTED',
            'ASSIGNED', 'ACCEPTED', 'RESPONDING',
            'ON_SCENE', 'RESOLVED', 'CANCELLED'
        )),

    changed_by UUID
        REFERENCES profiles(id) ON DELETE SET NULL,
    change_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_status_history_incident
    ON incident_status_history (incident_id, created_at DESC);

-- Backend must update incidents.current_status and insert the
-- corresponding history row in the same transaction. It must validate
-- allowed transitions and roll back if either operation fails.

-- =========================================================
-- 11. AUDIT LOGS
-- =========================================================

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_profile_id UUID
        REFERENCES profiles(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_entity
    ON audit_logs (entity_type, entity_id, created_at DESC);

-- =========================================================
-- 12. OPTIONAL INACTIVE TEAM PLACEHOLDERS
-- These are configuration placeholders only, not confirmation
-- that the named agencies formally participate in ARMS.
-- An authorized administrator must verify and activate them.
-- =========================================================

INSERT INTO responder_teams
    (team_name, team_type, description, is_active)
VALUES
    ('Valencia City CDRRMO', 'CDRRMO',
     'Placeholder; verify participation before activation', FALSE),
    ('BFP Valencia Station', 'BFP',
     'Placeholder; verify participation before activation', FALSE),
    ('PNP Valencia Station', 'PNP',
     'Placeholder; verify participation before activation', FALSE),
    ('Valencia City Medical Response', 'MEDICAL_EMS',
     'Placeholder; verify authorized medical response arrangements', FALSE),
    ('Philippine Red Cross - Bukidnon Chapter', 'RED_CROSS',
     'Placeholder; verify participation before activation', FALSE)
ON CONFLICT (team_name) DO NOTHING;

COMMIT;
