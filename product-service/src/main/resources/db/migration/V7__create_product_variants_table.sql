CREATE TABLE product_variants (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    product_id UUID NOT NULL,

    sku VARCHAR(100) NOT NULL UNIQUE,

    price DECIMAL(10,2) NOT NULL,

    discount_price DECIMAL(10,2),

    currency VARCHAR(3) NOT NULL DEFAULT 'USD',

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_product_variants_product
        FOREIGN KEY (product_id)
        REFERENCES products (id)
        ON DELETE CASCADE

);

CREATE INDEX idx_product_variants_product_id
    ON product_variants (product_id);