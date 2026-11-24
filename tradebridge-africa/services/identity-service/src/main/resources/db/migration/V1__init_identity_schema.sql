-- TradeBridge Africa — Identity Service schema
-- Spec §3.2. Tables created in dependency order. No cross-service FKs.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================ users
CREATE TABLE users (
    id                  UUID         PRIMARY KEY,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    phone_number        VARCHAR(30)  NOT NULL,
    country_code        CHAR(2)      NOT NULL,
    preferred_language  VARCHAR(5)   NOT NULL,
    status              VARCHAR(30)  NOT NULL,
    email_verified      BOOLEAN      NOT NULL DEFAULT FALSE,
    phone_verified      BOOLEAN      NOT NULL DEFAULT FALSE,
    mfa_enabled         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    row_version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_phone UNIQUE (phone_number),
    CONSTRAINT ck_users_status CHECK (status IN
        ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'LOCKED', 'CLOSED'))
);

CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_users_created_at ON users (created_at DESC);

-- ============================================================ user_credentials
CREATE TABLE user_credentials (
    id                    UUID         PRIMARY KEY,
    user_id               UUID         NOT NULL,
    password_hash         VARCHAR(255) NOT NULL,
    password_changed_at   TIMESTAMPTZ,
    password_expires_at   TIMESTAMPTZ,
    failed_attempts       INTEGER      NOT NULL DEFAULT 0,
    locked_until          TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_user_credentials_user UNIQUE (user_id)
);

-- ============================================================ roles
CREATE TABLE roles (
    id          UUID         PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    system_role BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_roles_code UNIQUE (code)
);

-- ============================================================ permissions
CREATE TABLE permissions (
    id          UUID         PRIMARY KEY,
    code        VARCHAR(100) NOT NULL,
    resource    VARCHAR(100) NOT NULL,
    action      VARCHAR(50)  NOT NULL,
    description TEXT,
    CONSTRAINT uk_permissions_code UNIQUE (code)
);

-- ============================================================ user_roles
CREATE TABLE user_roles (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL,
    role_id     UUID         NOT NULL,
    assigned_by UUID,
    assigned_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ,
    CONSTRAINT uk_user_roles UNIQUE (user_id, role_id)
);
CREATE INDEX idx_user_roles_user ON user_roles (user_id);
CREATE INDEX idx_user_roles_role ON user_roles (role_id);

-- ============================================================ role_permissions
CREATE TABLE role_permissions (
    id            UUID PRIMARY KEY,
    role_id       UUID NOT NULL,
    permission_id UUID NOT NULL,
    CONSTRAINT uk_role_permissions UNIQUE (role_id, permission_id)
);
CREATE INDEX idx_role_permissions_role ON role_permissions (role_id);

-- ============================================================ mfa_factors
CREATE TABLE mfa_factors (
    id               UUID        PRIMARY KEY,
    user_id          UUID        NOT NULL,
    factor_type      VARCHAR(30) NOT NULL,
    secret_encrypted TEXT,
    verified         BOOLEAN     NOT NULL DEFAULT FALSE,
    enabled          BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_used_at     TIMESTAMPTZ,
    CONSTRAINT ck_mfa_factor_type CHECK (factor_type IN ('TOTP', 'EMAIL_OTP'))
);
CREATE INDEX idx_mfa_factors_user ON mfa_factors (user_id);

-- ============================================================ refresh_tokens
CREATE TABLE refresh_tokens (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL,
    token_hash  VARCHAR(255) NOT NULL,
    session_id  UUID         NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash)
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_session ON refresh_tokens (session_id);
CREATE INDEX idx_refresh_tokens_expiry ON refresh_tokens (expires_at);

-- ============================================================ user_sessions
CREATE TABLE user_sessions (
    id               UUID         PRIMARY KEY,
    user_id          UUID         NOT NULL,
    device_name      VARCHAR(150),
    ip_address       INET,
    user_agent       TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_activity_at TIMESTAMPTZ,
    expires_at       TIMESTAMPTZ  NOT NULL,
    revoked_at       TIMESTAMPTZ
);
CREATE INDEX idx_user_sessions_user_active
    ON user_sessions (user_id, expires_at) WHERE revoked_at IS NULL;

-- ============================================================ verification_challenges
CREATE TABLE verification_challenges (
    id                   UUID         PRIMARY KEY,
    user_id              UUID         NOT NULL,
    challenge_type       VARCHAR(40)  NOT NULL,
    destination_masked   VARCHAR(255),
    code_hash            VARCHAR(255),
    expires_at           TIMESTAMPTZ  NOT NULL,
    attempt_count        INTEGER      NOT NULL DEFAULT 0,
    consumed_at          TIMESTAMPTZ,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_challenge_type CHECK (challenge_type IN
        ('EMAIL_VERIFICATION', 'PASSWORD_RESET', 'MFA_EMAIL_OTP',
         'MFA_DISABLE', 'RECOVERY_CODE_REGENERATE', 'RECENT_AUTHENTICATION'))
);
CREATE INDEX idx_challenges_user_type
    ON verification_challenges (user_id, challenge_type, created_at DESC);

-- ============================================================ password_reset_tokens
CREATE TABLE password_reset_tokens (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL,
    token_hash  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_password_reset_tokens_hash UNIQUE (token_hash)
);
CREATE INDEX idx_password_reset_user ON password_reset_tokens (user_id);

-- ============================================================ recovery_codes
CREATE TABLE recovery_codes (
    id         UUID         PRIMARY KEY,
    user_id    UUID         NOT NULL,
    code_hash  VARCHAR(255) NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_recovery_codes_user_unused
    ON recovery_codes (user_id) WHERE used_at IS NULL;

-- ============================================================ identity_audit_records
CREATE TABLE identity_audit_records (
    id          UUID         PRIMARY KEY,
    user_id     UUID,
    actor_id    UUID,
    action      VARCHAR(100) NOT NULL,
    ip_address  INET,
    metadata    JSONB,
    occurred_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_identity_audit_user
    ON identity_audit_records (user_id, occurred_at DESC);
CREATE INDEX idx_identity_audit_action
    ON identity_audit_records (action, occurred_at DESC);
