CREATE TABLE sellers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_id VARCHAR(36) NOT NULL UNIQUE,
    store_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),
    description TEXT,
    logo_url VARCHAR(500),
    status seller_status NOT NULL DEFAULT 'ACTIVE', --> 'PENDING_REVIEW'
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_sellers_keycloak_id ON sellers(keycloak_id);
CREATE INDEX idx_sellers_email ON sellers(email);
CREATE INDEX idx_sellers_status ON sellers(status);