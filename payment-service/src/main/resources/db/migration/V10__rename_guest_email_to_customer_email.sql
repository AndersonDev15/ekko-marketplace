ALTER TABLE payments RENAME COLUMN guest_email TO customer_email;

ALTER TABLE payments DROP CONSTRAINT chk_payments_customer_or_guest;

ALTER TABLE payments ADD CONSTRAINT chk_payments_customer_email_not_blank
    CHECK (customer_email IS NOT NULL AND length(btrim(customer_email)) > 0);