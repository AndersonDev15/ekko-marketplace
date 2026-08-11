CREATE TABLE refunds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL,
    stripe_refund_id VARCHAR(255) UNIQUE,
    amount DECIMAL(10, 2) NOT NULL,
    reason VARCHAR(255),
    status refund_status NOT NULL DEFAULT 'PENDING',
    requested_by UUID NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_refunds_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments(id)
);

CREATE INDEX idx_refunds_payment_id
    ON refunds(payment_id);

CREATE INDEX idx_refunds_status
    ON refunds(status);