CREATE UNIQUE INDEX uk_seller_address_primary
ON seller_addresses (seller_id)
WHERE is_primary = true;