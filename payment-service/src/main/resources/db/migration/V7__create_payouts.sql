CREATE TABLE payouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_id UUID NOT NULL,
    stripe_account_id VARCHAR(255) NOT NULL,
    stripe_payout_id VARCHAR(255) UNIQUE,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status payout_status NOT NULL DEFAULT 'PENDING',
    arrival_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_payouts_vendor_id
    ON payouts(vendor_id);

CREATE INDEX idx_payouts_status
    ON payouts(status);