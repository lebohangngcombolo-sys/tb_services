-- TradeBridge Africa — Country Service schema
-- Spec §4.2. No cross-service FKs (per §1.3).

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================ countries
CREATE TABLE countries (
    id                     UUID         PRIMARY KEY,
    iso2_code              CHAR(2)      NOT NULL,
    iso3_code              CHAR(3)      NOT NULL,
    name                   VARCHAR(150) NOT NULL,
    region                 VARCHAR(100),
    calling_code           VARCHAR(10),
    default_currency_code  CHAR(3)      NOT NULL,
    default_language_code  VARCHAR(5)   NOT NULL,
    enabled                BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    row_version            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_countries_iso2 UNIQUE (iso2_code),
    CONSTRAINT uk_countries_iso3 UNIQUE (iso3_code),
    CONSTRAINT ck_countries_iso2_fmt CHECK (iso2_code ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_countries_iso3_fmt CHECK (iso3_code ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_countries_enabled ON countries (enabled) WHERE enabled = TRUE;
CREATE INDEX idx_countries_name ON countries (name);

-- ============================================================ currencies
CREATE TABLE currencies (
    code            CHAR(3)      PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    symbol          VARCHAR(10),
    decimal_places  SMALLINT     NOT NULL DEFAULT 2,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_currencies_code_fmt CHECK (code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_currencies_decimals CHECK (decimal_places BETWEEN 0 AND 4)
);

-- ============================================================ country_currencies
CREATE TABLE country_currencies (
    id             UUID    PRIMARY KEY,
    country_code   CHAR(2) NOT NULL,
    currency_code  CHAR(3) NOT NULL,
    is_default     BOOLEAN NOT NULL DEFAULT FALSE,
    enabled        BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_country_currencies UNIQUE (country_code, currency_code)
);

CREATE INDEX idx_country_currencies_country ON country_currencies (country_code);
CREATE INDEX idx_country_currencies_default
    ON country_currencies (country_code) WHERE is_default = TRUE;

-- ============================================================ languages
CREATE TABLE languages (
    code         VARCHAR(5)   PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    native_name  VARCHAR(100),
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE
);

-- ============================================================ country_languages
CREATE TABLE country_languages (
    id             UUID       PRIMARY KEY,
    country_code   CHAR(2)    NOT NULL,
    language_code  VARCHAR(5) NOT NULL,
    is_default     BOOLEAN    NOT NULL DEFAULT FALSE,
    enabled        BOOLEAN    NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_country_languages UNIQUE (country_code, language_code)
);

CREATE INDEX idx_country_languages_country ON country_languages (country_code);
CREATE INDEX idx_country_languages_default
    ON country_languages (country_code) WHERE is_default = TRUE;

-- ============================================================ country_configurations
CREATE TABLE country_configurations (
    id                  UUID         PRIMARY KEY,
    country_code        CHAR(2)      NOT NULL,
    configuration_key   VARCHAR(150) NOT NULL,
    configuration_value JSONB        NOT NULL,
    version             INTEGER      NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    effective_from      TIMESTAMPTZ,
    effective_until     TIMESTAMPTZ,
    created_by          UUID,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_country_config UNIQUE (country_code, configuration_key, version),
    CONSTRAINT ck_country_config_status CHECK (status IN
        ('DRAFT', 'PUBLISHED', 'SUPERSEDED', 'ROLLED_BACK')),
    CONSTRAINT ck_country_config_version CHECK (version > 0)
);

CREATE INDEX idx_country_config_country_version
    ON country_configurations (country_code, version);
CREATE INDEX idx_country_config_country_status
    ON country_configurations (country_code, status);

-- ============================================================ organization_requirements
CREATE TABLE organization_requirements (
    id                  UUID         PRIMARY KEY,
    country_code        CHAR(2)      NOT NULL,
    organization_type   VARCHAR(50)  NOT NULL,
    requirement_code    VARCHAR(100) NOT NULL,
    description         TEXT,
    required            BOOLEAN      NOT NULL,
    validation_rule     JSONB,
    effective_from      TIMESTAMPTZ,
    effective_until     TIMESTAMPTZ,
    enabled             BOOLEAN      NOT NULL DEFAULT TRUE,
    row_version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_org_requirements UNIQUE
        (country_code, organization_type, requirement_code)
);

CREATE INDEX idx_org_req_country
    ON organization_requirements (country_code) WHERE enabled = TRUE;

-- ============================================================ kyc_requirements
CREATE TABLE kyc_requirements (
    id                    UUID         PRIMARY KEY,
    country_code          CHAR(2)      NOT NULL,
    customer_type         VARCHAR(30)  NOT NULL,
    document_type         VARCHAR(60)  NOT NULL,
    requirement_code      VARCHAR(100) NOT NULL,
    description           TEXT,
    required              BOOLEAN      NOT NULL,
    allowed_mime_types    JSONB,
    max_file_size_bytes   BIGINT,
    validity_days         INTEGER,
    enabled               BOOLEAN      NOT NULL DEFAULT TRUE,
    row_version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_kyc_requirements UNIQUE
        (country_code, customer_type, requirement_code),
    CONSTRAINT ck_kyc_file_size CHECK (max_file_size_bytes IS NULL OR max_file_size_bytes > 0),
    CONSTRAINT ck_kyc_validity CHECK (validity_days IS NULL OR validity_days > 0)
);

CREATE INDEX idx_kyc_req_country
    ON kyc_requirements (country_code) WHERE enabled = TRUE;

-- ============================================================ configuration_versions
CREATE TABLE configuration_versions (
    id              UUID         PRIMARY KEY,
    country_code    CHAR(2)      NOT NULL,
    version         INTEGER      NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    change_summary  TEXT,
    published_by    UUID,
    published_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_config_versions UNIQUE (country_code, version),
    CONSTRAINT ck_config_versions_status CHECK (status IN
        ('DRAFT', 'PUBLISHED', 'SUPERSEDED', 'ROLLED_BACK')),
    CONSTRAINT ck_config_versions_version CHECK (version > 0)
);

CREATE INDEX idx_config_versions_country ON configuration_versions (country_code);
CREATE INDEX idx_config_versions_published
    ON configuration_versions (country_code) WHERE status = 'PUBLISHED';