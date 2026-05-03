ALTER TABLE products
DROP CONSTRAINT IF EXISTS products_name_key;

ALTER TABLE products
    ADD CONSTRAINT uq_product_name_brand_color
        UNIQUE (name, brand, color);