ALTER TABLE products
ADD COLUMN seller_slug VARCHAR(100);

CREATE INDEX idx_products_seller_slug
ON products (seller_slug);