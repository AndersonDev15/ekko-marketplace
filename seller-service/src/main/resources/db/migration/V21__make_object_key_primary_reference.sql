ALTER TABLE seller_documents DROP COLUMN document_url;
ALTER TABLE seller_documents ALTER COLUMN object_key SET NOT NULL;