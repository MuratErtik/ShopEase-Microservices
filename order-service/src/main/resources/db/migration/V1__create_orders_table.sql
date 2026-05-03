CREATE TABLE orders
(
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id          UUID         NOT NULL,
    status           VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    total_amount     NUMERIC(10,2) NOT NULL,
    shipping_address TEXT         NOT NULL,
    card_holder_name VARCHAR(100),
    card_number      VARCHAR(20),
    expire_month     VARCHAR(2),
    expire_year      VARCHAR(4),
    cvc              VARCHAR(4),
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    version          BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT chk_order_status CHECK (status IN (
                                                  'PENDING',
                                                  'STOCK_RESERVED',
                                                  'CONFIRMED',
                                                  'CANCELLED'
        )),
    CONSTRAINT chk_total_amount CHECK (total_amount > 0)
);

CREATE INDEX idx_orders_user_id     ON orders (user_id);
CREATE INDEX idx_orders_status      ON orders (status);
CREATE INDEX idx_orders_user_status ON orders (user_id, status);