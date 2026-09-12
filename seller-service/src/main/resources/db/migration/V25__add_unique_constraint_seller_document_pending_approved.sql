ALTER TABLE seller_documents
DROP CONSTRAINT IF EXISTS uk_seller_document_type;

CREATE UNIQUE INDEX IF NOT EXISTS uk_seller_document_pending
ON seller_documents (seller_id, document_type)
WHERE status = 'PENDING';

CREATE UNIQUE INDEX uk_seller_document_approved
ON seller_documents (seller_id, document_type)
WHERE status = 'APPROVED';