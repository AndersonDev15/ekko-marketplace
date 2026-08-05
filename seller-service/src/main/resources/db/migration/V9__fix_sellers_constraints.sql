-- Eliminar constraints autogenerados
ALTER TABLE sellers DROP CONSTRAINT IF EXISTS sellers_keycloak_id_key;
ALTER TABLE sellers DROP CONSTRAINT IF EXISTS sellers_email_key;

-- Recrear con nombres explícitos
ALTER TABLE sellers ADD CONSTRAINT uk_sellers_keycloak_id UNIQUE (keycloak_id);
ALTER TABLE seller_documents ADD CONSTRAINT uk_seller_document_type UNIQUE (seller_id, document_type);
ALTER TABLE seller_bank_accounts ADD CONSTRAINT uk_seller_bank_account UNIQUE (seller_id, bank_name, account_number);

-- Corregir default de status
ALTER TABLE sellers ALTER COLUMN status SET DEFAULT 'PENDING_REVIEW';