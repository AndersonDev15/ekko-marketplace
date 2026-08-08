CREATE TABLE seller_snapshot (

    seller_keycloak_id UUID PRIMARY KEY,

    store_name VARCHAR(255) NOT NULL,

    email VARCHAR(255) NOT NULL,

    updated_at TIMESTAMP NOT NULL

);