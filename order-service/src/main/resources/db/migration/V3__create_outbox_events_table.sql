CREATE TABLE outbox_events
(
    id             UUID        NOT NULL DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id   VARCHAR(36) NOT NULL,
    event_type     VARCHAR(50) NOT NULL,
    target_system  VARCHAR(50) NOT NULL,
    payload        JSONB       NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count    INTEGER     NOT NULL DEFAULT 0,
    created_at     TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_outbox_events PRIMARY KEY (id),
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    CONSTRAINT chk_retry_count   CHECK (retry_count >= 0)
);

CREATE INDEX idx_outbox_status     ON outbox_events (status);
CREATE INDEX idx_outbox_created_at ON outbox_events (created_at);

