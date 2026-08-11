CREATE TABLE payment_transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL,
    vendor_id UUID NOT NULL,
    stripe_transfer_id VARCHAR(255) UNIQUE,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status transfer_status NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_payment_transfers_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments(id)
);

CREATE INDEX idx_transfers_payment_id
    ON payment_transfers(payment_id);

CREATE INDEX idx_transfers_vendor_id
    ON payment_transfers(vendor_id);