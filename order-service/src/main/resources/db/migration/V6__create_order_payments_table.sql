CREATE TABLE order_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    order_id UUID NOT NULL,

    payment_id VARCHAR(255),
    payment_intent_id VARCHAR(255),

    amount DECIMAL(10, 2) NOT NULL,

    currency VARCHAR(3) NOT NULL DEFAULT 'USD',

    status payment_status NOT NULL DEFAULT 'PENDING',

    payment_method VARCHAR(50),

    is_current BOOLEAN NOT NULL DEFAULT TRUE,

    paid_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_order_payments_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE
);