package com.ekko.product_service.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class SellerNotOperationalException extends RuntimeException {

    private final UUID sellerKeycloakId;

    public SellerNotOperationalException(UUID sellerKeycloakId) {
        super("Seller " + sellerKeycloakId + " is not active and cannot perform this operation.");
        this.sellerKeycloakId = sellerKeycloakId;
    }

}