-- TradeBridge Africa — Notification Service schema
-- Spec §5.2. No cross-service FKs.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================ notification_templates
CREATE TABLE notification_templates (
    id                UUID         PRIMARY KEY,
    template_code     VARCHAR(100) NOT NULL,
    channel           VARCHAR(20)  NOT NULL,
    language_code     VARCHAR(5)   NOT NULL,
    subject_template  TEXT,
    body_template     TEXT         NOT NULL,
    variables_schema  JSONB,
    version           INTEGER      NOT NULL DEFAULT 1,
    enabled           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    row_version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_notification_templates
        UNIQUE (template_code, channel, language_code, version),
    CONSTRAINT ck_notification_templates_channel
        CHECK (channel IN ('EMAIL', 'SMS', 'IN_APP')),
    CONSTRAINT ck_notification_templates_version CHECK (version >= 1)
);

CREATE INDEX idx_notification_templates_lookup
    ON notification_templates (template_code, channel, language_code)
    WHERE enabled = TRUE;

-- ============================================================ notifications
CREATE TABLE notifications (
    id                    UUID         PRIMARY KEY,
    recipient_user_id     UUID,
    recipient_reference   VARCHAR(255),
    template_code         VARCHAR(100),
    channel               VARCHAR(20)  NOT NULL,
    language_code         VARCHAR(5)   NOT NULL,
    subject               TEXT,
    body_reference        UUID,
    status                VARCHAR(30)  NOT NULL,
    priority              VARCHAR(20)  NOT NULL,
    source_service        VARCHAR(100) NOT NULL,
    correlation_id        UUID,
    idempotency_key       VARCHAR(150),
    scheduled_at          TIMESTAMPTZ,
    expires_at            TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    row_version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_notifications_channel
        CHECK (channel IN ('EMAIL', 'SMS', 'IN_APP')),
    CONSTRAINT ck_notifications_status
        CHECK (status IN ('PENDING', 'SCHEDULED', 'SENT', 'DELIVERED', 'FAILED', 'EXHAUSTED')),
    CONSTRAINT ck_notifications_priority
        CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'CRITICAL'))
);

CREATE UNIQUE INDEX uk_notifications_idempotency
    ON notifications (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX idx_notifications_recipient
    ON notifications (recipient_user_id, created_at DESC);
CREATE INDEX idx_notifications_status
    ON notifications (status, created_at);
CREATE INDEX idx_notifications_source
    ON notifications (source_service, created_at DESC);

-- ============================================================ notification_recipients
CREATE TABLE notification_recipients (
    id                    UUID         PRIMARY KEY,
    notification_id       UUID         NOT NULL,
    user_id               UUID,
    destination_type      VARCHAR(20)  NOT NULL,
    destination_masked    VARCHAR(255),
    destination_hash      VARCHAR(255),
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_notification_recipients_type
        CHECK (destination_type IN ('EMAIL', 'SMS', 'IN_APP'))
);

CREATE INDEX idx_notification_recipients_notification
    ON notification_recipients (notification_id);
CREATE INDEX idx_notification_recipients_hash
    ON notification_recipients (destination_hash);

-- ============================================================ notification_deliveries
CREATE TABLE notification_deliveries (
    id                    UUID         PRIMARY KEY,
    notification_id       UUID         NOT NULL,
    provider              VARCHAR(50)  NOT NULL,
    provider_message_id   VARCHAR(255),
    status                VARCHAR(30)  NOT NULL,
    attempt_number        INTEGER      NOT NULL,
    failure_code          VARCHAR(100),
    failure_reason        TEXT,
    submitted_at          TIMESTAMPTZ,
    delivered_at          TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_notification_deliveries_status
        CHECK (status IN ('ACCEPTED', 'DELIVERED', 'BOUNCED', 'FAILED')),
    CONSTRAINT ck_notification_deliveries_attempt CHECK (attempt_number >= 1)
);

CREATE INDEX idx_notification_deliveries_notification
    ON notification_deliveries (notification_id, attempt_number DESC);
CREATE INDEX idx_notification_deliveries_provider
    ON notification_deliveries (provider, created_at DESC);
CREATE INDEX idx_notification_deliveries_status
    ON notification_deliveries (status, created_at);

-- ============================================================ notification_preferences
CREATE TABLE notification_preferences (
    id               UUID         PRIMARY KEY,
    user_id          UUID         NOT NULL,
    channel          VARCHAR(20)  NOT NULL,
    enabled          BOOLEAN      NOT NULL,
    language_code    VARCHAR(5),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_notification_preferences UNIQUE (user_id, channel),
    CONSTRAINT ck_notification_preferences_channel
        CHECK (channel IN ('EMAIL', 'SMS', 'IN_APP'))
);

-- ============================================================ notification_logs
CREATE TABLE notification_logs (
    id               UUID         PRIMARY KEY,
    notification_id  UUID         NOT NULL,
    delivery_id      UUID,
    action           VARCHAR(100) NOT NULL,
    result           VARCHAR(30)  NOT NULL,
    metadata         JSONB,
    occurred_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_notification_logs_notification
    ON notification_logs (notification_id, occurred_at DESC);
CREATE INDEX idx_notification_logs_action
    ON notification_logs (action, occurred_at DESC);