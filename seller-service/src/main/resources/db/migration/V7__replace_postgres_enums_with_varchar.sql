-- sellers

ALTER TABLE sellers
ALTER COLUMN status DROP DEFAULT;

ALTER TABLE sellers
ALTER COLUMN status TYPE VARCHAR(20)
USING status::text;

ALTER TABLE sellers
ALTER COLUMN status SET DEFAULT 'ACTIVE';



-- seller_documents

ALTER TABLE seller_documents
ALTER COLUMN document_type TYPE VARCHAR(50)
USING document_type::text;

ALTER TABLE seller_documents
ALTER COLUMN status DROP DEFAULT;

ALTER TABLE seller_documents
ALTER COLUMN status TYPE VARCHAR(20)
USING status::text;

ALTER TABLE seller_documents
ALTER COLUMN status SET DEFAULT 'PENDING';



-- drop enums

DROP TYPE seller_status;
DROP TYPE document_type;
DROP TYPE document_status;