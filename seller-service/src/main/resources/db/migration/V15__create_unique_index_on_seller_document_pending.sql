CREATE UNIQUE INDEX uk_seller_document_pending
ON seller_documents (seller_id, document_type)
WHERE status = 'PENDING';