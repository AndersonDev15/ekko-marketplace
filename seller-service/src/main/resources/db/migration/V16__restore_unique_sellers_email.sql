-- Restaurar la restricción UNIQUE sobre email eliminada en V9.
-- Recrear con nombre explícito, consistente con uk_sellers_keycloak_id.
ALTER TABLE sellers
    ADD CONSTRAINT uk_sellers_email UNIQUE (email);
