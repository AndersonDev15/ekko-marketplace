CREATE TABLE payment_vendor_allocations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL,
    vendor_id UUID NOT NULL,
    gross_amount DECIMAL(10, 2) NOT NULL,
    application_fee_amount DECIMAL(10, 2) NOT NULL,
    net_amount DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_payment_vendor_allocations_payment
        FOREIGN KEY (payment_id) REFERENCES payments(id)
);

CREATE INDEX idx_payment_vendor_allocations_payment_id ON payment_vendor_allocations(payment_id);
CREATE INDEX idx_payment_vendor_allocations_vendor_id ON payment_vendor_allocations(vendor_id);
