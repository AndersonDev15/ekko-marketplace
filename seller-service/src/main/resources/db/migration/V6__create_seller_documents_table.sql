CREATE TABLE seller_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id UUID NOT NULL REFERENCES sellers(id) ON DELETE CASCADE,
    document_type document_type NOT NULL,
    document_url VARCHAR(500) NOT NULL,
    status document_status NOT NULL DEFAULT 'PENDING',
    uploaded_at TIMESTAMP NOT NULL DEFAULT now(),
    reviewed_at TIMESTAMP,
    reviewed_by VARCHAR(36),
    notes TEXT
);

CREATE INDEX idx_documents_seller_id ON seller_documents(seller_id);
CREATE INDEX idx_documents_status ON seller_documents(status);