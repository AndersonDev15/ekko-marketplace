CREATE TABLE order_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    order_id UUID NOT NULL UNIQUE,

    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,

    address_line VARCHAR(500) NOT NULL,

    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,

    postal_code VARCHAR(20),

    CONSTRAINT fk_order_addresses_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE
);