-- TradeBridge Africa — Country Service outbox
-- Country Service produces events but does not consume any (spec §4.3).
-- Inbox table is therefore omitted; add it later if a consumer is introduced.

CREATE TABLE country_outbox (
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
    CONSTRAINT ck_country_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT uk_country_outbox_event UNIQUE (event_id)
);

CREATE INDEX idx_country_outbox_pending
    ON country_outbox (created_at ASC) WHERE status = 'PENDING';
CREATE INDEX idx_country_outbox_status_created
    ON country_outbox (status, created_at);
CREATE INDEX idx_country_outbox_aggregate
    ON country_outbox (aggregate_id);