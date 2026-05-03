CREATE TABLE order_items
(
    id          UUID          NOT NULL DEFAULT gen_random_uuid(),
    order_id    UUID          NOT NULL,
    product_id  UUID          NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity    INTEGER       NOT NULL,
    unit_price  NUMERIC(10,2) NOT NULL,
    total_price NUMERIC(10,2) NOT NULL,

    CONSTRAINT pk_order_items PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT chk_quantity   CHECK (quantity > 0),
    CONSTRAINT chk_unit_price CHECK (unit_price > 0),
    CONSTRAINT chk_total_price CHECK (total_price > 0)
);

CREATE INDEX idx_order_items_order_id   ON order_items (order_id);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);


