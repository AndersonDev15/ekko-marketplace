CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    order_id UUID NOT NULL,

    variant_id UUID NOT NULL,
    product_id UUID NOT NULL,

    product_name_snapshot VARCHAR(255) NOT NULL,
    variant_snapshot VARCHAR(255),

    seller_id UUID NOT NULL,
    seller_name_snapshot VARCHAR(255) NOT NULL,

    price_snapshot DECIMAL(10, 2) NOT NULL,

    quantity INTEGER NOT NULL,

    subtotal DECIMAL(10, 2) NOT NULL,

    image_url_snapshot VARCHAR(1000),

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE
);