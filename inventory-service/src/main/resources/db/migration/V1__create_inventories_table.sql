CREATE TABLE inventories
(
    id                 UUID         NOT NULL DEFAULT gen_random_uuid(),
    product_id         UUID         NOT NULL,
    seller_id          UUID         NOT NULL,
    available_quantity INTEGER      NOT NULL DEFAULT 0,
    reserved_quantity  INTEGER      NOT NULL DEFAULT 0,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    version            BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_inventories PRIMARY KEY (id),
    CONSTRAINT uq_inventories_product_id UNIQUE (product_id),
    CONSTRAINT chk_available_quantity CHECK (available_quantity >= 0),
    CONSTRAINT chk_reserved_quantity CHECK (reserved_quantity >= 0)
);

CREATE INDEX idx_inventory_product_id ON inventories (product_id);
CREATE INDEX idx_inventory_seller_id  ON inventories (seller_id);

