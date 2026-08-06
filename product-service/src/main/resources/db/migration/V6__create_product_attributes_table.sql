CREATE TABLE product_attributes (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    product_id UUID NOT NULL,

    name VARCHAR(100) NOT NULL,

    value VARCHAR(255) NOT NULL,

    CONSTRAINT fk_product_attributes_product
        FOREIGN KEY (product_id)
        REFERENCES products (id)
        ON DELETE CASCADE

);