CREATE TABLE product_variant_attributes (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    variant_id UUID NOT NULL,

    name VARCHAR(100) NOT NULL,

    value VARCHAR(255) NOT NULL,

    CONSTRAINT fk_product_variant_attributes_variant
        FOREIGN KEY (variant_id)
        REFERENCES product_variants (id)
        ON DELETE CASCADE

);
