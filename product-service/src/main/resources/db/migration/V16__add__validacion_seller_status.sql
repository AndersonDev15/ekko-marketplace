CREATE TABLE seller_status_view (
    seller_keycloak_id UUID PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    updated_at TIMESTAMP NOT NULL
);