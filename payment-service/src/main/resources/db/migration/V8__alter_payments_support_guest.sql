ALTER TABLE payments ALTER COLUMN customer_id DROP NOT NULL;

ALTER TABLE payments ADD COLUMN guest_email VARCHAR(255);

ALTER TABLE payments ADD CONSTRAINT chk_payments_customer_or_guest
    CHECK (
        (customer_id IS NOT NULL AND guest_email IS NULL) OR
        (customer_id IS NULL AND guest_email IS NOT NULL)
    );