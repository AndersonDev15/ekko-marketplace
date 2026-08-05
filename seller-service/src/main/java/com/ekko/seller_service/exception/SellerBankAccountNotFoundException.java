package com.ekko.seller_service.exception;

import java.util.UUID;

public class SellerBankAccountNotFoundException extends SellerServiceException {

    public SellerBankAccountNotFoundException(UUID accountId) {
        super("Seller bank account not found with id: " + accountId);
    }

    public SellerBankAccountNotFoundException(UUID accountId, UUID sellerId) {
        super("Seller bank account " + accountId + " not found for seller: " + sellerId);
    }
}

