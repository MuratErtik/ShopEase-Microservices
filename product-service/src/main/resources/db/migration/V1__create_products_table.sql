CREATE TABLE products
(
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    version     BIGINT       NOT NULL DEFAULT 0,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    price       NUMERIC(10, 2) NOT NULL,
    brand       VARCHAR(255) NOT NULL,
    color       VARCHAR(30)  NOT NULL,
    category    VARCHAR(100) NOT NULL,
    image_url   VARCHAR(500),
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT chk_price_positive CHECK (price > 0)
);

CREATE INDEX idx_product_status ON products (status);
CREATE INDEX idx_product_category ON products (category);
CREATE INDEX idx_product_name ON products (name);
CREATE INDEX idx_product_price ON products (price);