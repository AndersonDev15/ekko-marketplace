CREATE TABLE inventory (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    variant_id UUID NOT NULL UNIQUE,

    stock_available BIGINT NOT NULL DEFAULT 0,

    stock_reserved BIGINT NOT NULL DEFAULT 0,

    stock_minimum BIGINT NOT NULL DEFAULT 5,

    updated_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_inventory_variant
        FOREIGN KEY (variant_id)
        REFERENCES product_variants (id)
        ON DELETE CASCADE

);

CREATE INDEX idx_inventory_variant_id
    ON inventory (variant_id);