CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL,
    type transaction_type NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status transaction_status NOT NULL,
    stripe_event_id VARCHAR(255) UNIQUE,
    error_message TEXT,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_payment_transactions_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments(id)
);

CREATE INDEX idx_transactions_payment_id
    ON payment_transactions(payment_id);

CREATE INDEX idx_transactions_type
    ON payment_transactions(type);