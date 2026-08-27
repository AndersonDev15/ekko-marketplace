package com.ekko.seller_service.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class SellerAlreadyActiveException extends RuntimeException {

    private final UUID sellerId;

    public SellerAlreadyActiveException(UUID sellerId) {
        super("Seller " + sellerId + " is already active and verified. " +
                "Profile data (documents, address, bank account) cannot be edited directly. " +
                "Contact support to request changes.");
        this.sellerId = sellerId;
    }

}
