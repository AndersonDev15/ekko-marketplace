package com.ekko.seller_service.exception;

import java.util.UUID;

public class SellerNotFoundException extends SellerServiceException {

    public SellerNotFoundException(UUID sellerId) {
        super("Seller not found with id: " + sellerId);
    }

    public SellerNotFoundException(String keycloakId) {
        super("Seller not found with keycloak_id: " + keycloakId);
    }
}
