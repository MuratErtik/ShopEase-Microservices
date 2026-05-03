CREATE TABLE outbox_events (
                               id UUID NOT NULL,
                               aggregate_type VARCHAR(255) NOT NULL,
                               aggregate_id VARCHAR(255) NOT NULL,
                               event_type VARCHAR(255) NOT NULL,
                               target_system VARCHAR(255) NOT NULL,
                               payload JSONB,
                               status VARCHAR(50) NOT NULL,
                               created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                               retry_count INTEGER DEFAULT 0,
                               CONSTRAINT pk_outbox_events PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_status ON outbox_events(status);