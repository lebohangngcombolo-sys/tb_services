-- TradeBridge Africa — Identity Service outbox + inbox
-- Tables backing the transactional outbox (§1.4) and idempotent consumer inbox.
-- Concrete entity classes: IdentityOutboxEntry, IdentityInboxEntry.

CREATE TABLE identity_outbox (
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
    CONSTRAINT ck_identity_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT uk_identity_outbox_event UNIQUE (event_id)
);

CREATE INDEX idx_identity_outbox_pending
    ON identity_outbox (created_at ASC) WHERE status = 'PENDING';
CREATE INDEX idx_identity_outbox_status_created
    ON identity_outbox (status, created_at);
CREATE INDEX idx_identity_outbox_aggregate
    ON identity_outbox (aggregate_id);

CREATE TABLE identity_inbox (
    id             UUID         PRIMARY KEY,
    event_id       UUID         NOT NULL,
    consumer_name  VARCHAR(100) NOT NULL,
    processed_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    result         VARCHAR(30)  NOT NULL DEFAULT 'OK',
    CONSTRAINT uk_identity_inbox_event_consumer UNIQUE (event_id, consumer_name)
);

CREATE INDEX idx_identity_inbox_processed
    ON identity_inbox (processed_at);
