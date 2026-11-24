-- TradeBridge Africa — Customer Service schema (spec §6.2)
-- No cross-service FKs per §1.3.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================ customer_profiles
CREATE TABLE customer_profiles (
    id                  UUID         PRIMARY KEY,
    identity_user_id    UUID         NOT NULL,
    customer_number     VARCHAR(40),
    customer_type       VARCHAR(30)  NOT NULL,
    country_code        CHAR(2)      NOT NULL,
    preferred_language  VARCHAR(5)   NOT NULL,
    status              VARCHAR(30)  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    row_version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_customer_profiles_identity UNIQUE (identity_user_id),
    CONSTRAINT uk_customer_profiles_number UNIQUE (customer_number),
    CONSTRAINT ck_customer_profiles_type
        CHECK (customer_type IN ('INDIVIDUAL', 'COMPANY_REPRESENTATIVE')),
    CONSTRAINT ck_customer_profiles_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'CLOSED'))
);

CREATE INDEX idx_customer_profiles_country ON customer_profiles (country_code);
CREATE INDEX idx_customer_profiles_status ON customer_profiles (status);

-- ============================================================ organizations
CREATE TABLE organizations (
    id                      UUID         PRIMARY KEY,
    organization_number     VARCHAR(50),
    legal_name              VARCHAR(255) NOT NULL,
    trading_name            VARCHAR(255),
    organization_type       VARCHAR(50)  NOT NULL,
    registration_number     VARCHAR(100) NOT NULL,
    tax_number              VARCHAR(100),
    country_code            CHAR(2)      NOT NULL,
    incorporation_date      DATE,
    registration_authority  VARCHAR(150),
    business_description    TEXT,
    website                 VARCHAR(255),
    status                  VARCHAR(30)  NOT NULL,
    created_by_user_id      UUID         NOT NULL,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    row_version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_organizations_number UNIQUE (organization_number),
    CONSTRAINT uk_organizations_registration
        UNIQUE (country_code, registration_authority, registration_number),
    CONSTRAINT ck_organizations_status CHECK (status IN
        ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED', 'WITHDRAWN'))
);

CREATE INDEX idx_organizations_status ON organizations (status);
CREATE INDEX idx_organizations_created_by ON organizations (created_by_user_id);

-- ============================================================ organization_addresses
CREATE TABLE organization_addresses (
    id                UUID         PRIMARY KEY,
    organization_id   UUID         NOT NULL,
    address_type      VARCHAR(30)  NOT NULL,
    address_line_1    VARCHAR(255) NOT NULL,
    address_line_2    VARCHAR(255),
    city              VARCHAR(100) NOT NULL,
    region            VARCHAR(100),
    postal_code       VARCHAR(30),
    country_code      CHAR(2)      NOT NULL,
    is_primary        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_organization_addresses_org ON organization_addresses (organization_id);

-- ============================================================ organization_representatives
CREATE TABLE organization_representatives (
    id                     UUID         PRIMARY KEY,
    organization_id        UUID         NOT NULL,
    identity_user_id       UUID,
    first_name             VARCHAR(100) NOT NULL,
    last_name              VARCHAR(100) NOT NULL,
    email                  VARCHAR(255) NOT NULL,
    phone_number           VARCHAR(30),
    representative_type    VARCHAR(40)  NOT NULL,
    is_authorized_signatory BOOLEAN     NOT NULL DEFAULT FALSE,
    verification_status    VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_org_representatives_type
        CHECK (representative_type IN ('DIRECTOR', 'OFFICER', 'SHAREHOLDER', 'SIGNATORY', 'OTHER')),
    CONSTRAINT ck_org_representatives_verification
        CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

CREATE INDEX idx_org_representatives_org ON organization_representatives (organization_id);
CREATE INDEX idx_org_representatives_identity ON organization_representatives (identity_user_id);
CREATE UNIQUE INDEX uk_org_representatives_email
    ON organization_representatives (organization_id, LOWER(email));

-- ============================================================ beneficial_owners
CREATE TABLE beneficial_owners (
    id                       UUID           PRIMARY KEY,
    organization_id          UUID           NOT NULL,
    first_name               VARCHAR(100)   NOT NULL,
    last_name                VARCHAR(100)   NOT NULL,
    date_of_birth            DATE,
    nationality_country_code CHAR(2),
    residence_country_code   CHAR(2),
    ownership_percentage     NUMERIC(7,4),
    control_type             VARCHAR(40)    NOT NULL,
    identity_reference_hash  VARCHAR(255),
    verification_status      VARCHAR(30)    NOT NULL DEFAULT 'PENDING',
    created_at               TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_beneficial_owners_control_type
        CHECK (control_type IN ('DIRECT', 'INDIRECT', 'CONTROL', 'TRUST', 'NOMINEE')),
    CONSTRAINT ck_beneficial_owners_percentage
        CHECK (ownership_percentage IS NULL
               OR (ownership_percentage >= 0 AND ownership_percentage <= 100)),
    CONSTRAINT ck_beneficial_owners_verification
        CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

CREATE INDEX idx_beneficial_owners_org ON beneficial_owners (organization_id);

-- ============================================================ kyc_cases
CREATE TABLE kyc_cases (
    id                            UUID         PRIMARY KEY,
    organization_id               UUID         NOT NULL,
    case_number                   VARCHAR(50),
    case_type                     VARCHAR(30)  NOT NULL,
    country_code                  CHAR(2)      NOT NULL,
    country_configuration_version INTEGER      NOT NULL,
    status                        VARCHAR(40)  NOT NULL,
    risk_level                    VARCHAR(20),
    assigned_reviewer_id          UUID,
    submitted_at                  TIMESTAMPTZ,
    completed_at                  TIMESTAMPTZ,
    created_at                    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    row_version                   BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_kyc_cases_number UNIQUE (case_number),
    CONSTRAINT ck_kyc_cases_type
        CHECK (case_type IN ('ONBOARDING', 'PERIODIC_REVIEW', 'ENHANCED_DUE_DILIGENCE')),
    CONSTRAINT ck_kyc_cases_status CHECK (status IN
        ('DRAFT', 'SUBMITTED', 'IN_REVIEW', 'INFORMATION_REQUESTED',
         'RESUBMITTED', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_kyc_cases_risk_level
        CHECK (risk_level IS NULL OR risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_kyc_cases_config_version CHECK (country_configuration_version >= 1)
);

CREATE INDEX idx_kyc_cases_org ON kyc_cases (organization_id);
CREATE INDEX idx_kyc_cases_status ON kyc_cases (status);
CREATE INDEX idx_kyc_cases_assigned ON kyc_cases (assigned_reviewer_id);

-- ============================================================ kyc_documents
CREATE TABLE kyc_documents (
    id                  UUID         PRIMARY KEY,
    kyc_case_id         UUID         NOT NULL,
    document_type       VARCHAR(60)  NOT NULL,
    requirement_code    VARCHAR(100) NOT NULL,
    storage_reference   VARCHAR(500) NOT NULL,
    original_filename   VARCHAR(255),
    mime_type           VARCHAR(100) NOT NULL,
    file_size_bytes     BIGINT       NOT NULL,
    checksum            VARCHAR(128),
    document_status     VARCHAR(30)  NOT NULL,
    uploaded_by_user_id UUID         NOT NULL,
    uploaded_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at          TIMESTAMPTZ,
    CONSTRAINT ck_kyc_documents_status CHECK (document_status IN
        ('PENDING', 'SCANNING', 'ACCEPTED', 'REJECTED', 'EXPIRED')),
    CONSTRAINT ck_kyc_documents_size CHECK (file_size_bytes > 0)
);

CREATE INDEX idx_kyc_documents_case ON kyc_documents (kyc_case_id);
CREATE INDEX idx_kyc_documents_status ON kyc_documents (document_status);

-- ============================================================ kyc_reviews
CREATE TABLE kyc_reviews (
    id                UUID         PRIMARY KEY,
    kyc_case_id       UUID         NOT NULL,
    reviewer_user_id  UUID         NOT NULL,
    review_stage      VARCHAR(30)  NOT NULL,
    review_status     VARCHAR(30)  NOT NULL,
    review_notes      TEXT,
    started_at        TIMESTAMPTZ,
    completed_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_kyc_reviews_case ON kyc_reviews (kyc_case_id);
CREATE INDEX idx_kyc_reviews_reviewer ON kyc_reviews (reviewer_user_id);

-- ============================================================ kyc_decisions
CREATE TABLE kyc_decisions (
    id                    UUID         PRIMARY KEY,
    kyc_case_id           UUID         NOT NULL,
    decision              VARCHAR(30)  NOT NULL,
    decision_reason_code  VARCHAR(100),
    decision_reason       TEXT,
    decided_by_user_id    UUID         NOT NULL,
    decision_version      INTEGER      NOT NULL,
    supersedes_decision_id UUID,
    decided_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_kyc_decisions_type CHECK (decision IN ('APPROVED', 'REJECTED')),
    CONSTRAINT ck_kyc_decisions_version CHECK (decision_version >= 1)
);

CREATE INDEX idx_kyc_decisions_case ON kyc_decisions (kyc_case_id);
CREATE INDEX idx_kyc_decisions_decided_by ON kyc_decisions (decided_by_user_id);

-- ============================================================ customer_status_history
CREATE TABLE customer_status_history (
    id                 UUID         PRIMARY KEY,
    customer_id        UUID,
    organization_id    UUID,
    old_status         VARCHAR(30),
    new_status         VARCHAR(30)  NOT NULL,
    reason             TEXT,
    changed_by_user_id UUID,
    changed_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_customer_status_history_target
        CHECK (customer_id IS NOT NULL OR organization_id IS NOT NULL)
);

CREATE INDEX idx_customer_status_history_customer
    ON customer_status_history (customer_id, changed_at DESC);
CREATE INDEX idx_customer_status_history_organization
    ON customer_status_history (organization_id, changed_at DESC);