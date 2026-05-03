CREATE TABLE payments
(
    id                 UUID          NOT NULL DEFAULT gen_random_uuid(),
    order_id           UUID          NOT NULL,
    user_id            UUID          NOT NULL,
    amount             NUMERIC(10,2) NOT NULL,
    status             VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    iyzico_payment_id  VARCHAR(100),
    failure_reason     TEXT,
    card_holder_name   VARCHAR(100),
    card_last_four     VARCHAR(4),
    created_at         TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP     NOT NULL DEFAULT NOW(),
    version            BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT uq_payments_order_id UNIQUE (order_id),
    CONSTRAINT chk_payment_status CHECK (status IN (
                                                    'PENDING',
                                                    'COMPLETED',
                                                    'FAILED'
        )),
    CONSTRAINT chk_amount CHECK (amount > 0)
);

CREATE INDEX idx_payments_order_id ON payments (order_id);
CREATE INDEX idx_payments_user_id  ON payments (user_id);
CREATE INDEX idx_payments_status   ON payments (status);

