CREATE UNIQUE INDEX uk_seller_bank_account_primary
ON seller_bank_accounts (seller_id)
WHERE is_primary = true;