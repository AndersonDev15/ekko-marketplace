ALTER TABLE sellers
ALTER COLUMN status DROP DEFAULT;

ALTER TABLE sellers
ALTER COLUMN status TYPE seller_status
USING status::seller_status;

ALTER TABLE sellers
ALTER COLUMN status SET DEFAULT 'PENDING_REVIEW';


ALTER TABLE seller_documents
ALTER COLUMN document_type TYPE document_type
USING document_type::document_type;


ALTER TABLE seller_documents
ALTER COLUMN status DROP DEFAULT;

ALTER TABLE seller_documents
ALTER COLUMN status TYPE document_status
USING status::document_status;

ALTER TABLE seller_documents
ALTER COLUMN status SET DEFAULT 'PENDING';


ALTER TABLE seller_bank_accounts
ALTER COLUMN account_type TYPE bank_account_type
USING account_type::bank_account_type;