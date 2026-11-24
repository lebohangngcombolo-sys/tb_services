-- TradeBridge Africa — Notification Service outbox + inbox

CREATE TABLE notification_outbox (
    id            UUID         PRIMARY KEY,
    aggregate_id  UUID         NOT NULL,
    event_id      UUID         NOT NULL,
    event_type    VARCHAR(150) NOT NULL,
    topic         VARCHAR(100) NOT NULL,
    payload       JSONB        NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    attempts      INTEGER      NOT NULL DEFAULT 0,
    last_error    TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at  TIMESTAMPTZ,
    row_version   BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_notification_outbox_status
        CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT uk_notification_outbox_event UNIQUE (event_id)
);

CREATE INDEX idx_notification_outbox_pending
    ON notification_outbox (created_at ASC) WHERE status = 'PENDING';
CREATE INDEX idx_notification_outbox_status_created
    ON notification_outbox (status, created_at);
CREATE INDEX idx_notification_outbox_aggregate
    ON notification_outbox (aggregate_id);

CREATE TABLE notification_inbox (
    id             UUID         PRIMARY KEY,
    event_id       UUID         NOT NULL,
    consumer_name  VARCHAR(100) NOT NULL,
    processed_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    result         VARCHAR(30)  NOT NULL DEFAULT 'OK',
    CONSTRAINT uk_notification_inbox_event_consumer
        UNIQUE (event_id, consumer_name)
);

CREATE INDEX idx_notification_inbox_processed
    ON notification_inbox (processed_at);