CREATE TABLE products (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    seller_keycloak_id UUID NOT NULL,
    brand_id UUID,
    category_id UUID NOT NULL,

    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,

    description TEXT,

    status product_status NOT NULL DEFAULT 'DRAFT',

    average_rating DECIMAL(2,1) NOT NULL DEFAULT 0,
    review_count INTEGER NOT NULL DEFAULT 0,

    deleted_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_products_brand
        FOREIGN KEY (brand_id)
        REFERENCES brands (id),

    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id)
        REFERENCES categories (id),

    CONSTRAINT uk_products_seller_slug
        UNIQUE (seller_keycloak_id, slug)

);

CREATE INDEX idx_products_seller_keycloak_id
    ON products (seller_keycloak_id);

CREATE INDEX idx_products_category_id
    ON products (category_id);

CREATE INDEX idx_products_status
    ON products (status);