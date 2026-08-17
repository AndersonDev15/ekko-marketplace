-- Review Service - V4: track seller per eligible order item
-- seller_service consumes review.created and needs to know which SellerMetrics to update.
-- order.confirmed always carries sellerKeycloakId per item, so the column is NOT NULL.

ALTER TABLE eligible_reviews
    ADD COLUMN seller_keycloak_id UUID NOT NULL;