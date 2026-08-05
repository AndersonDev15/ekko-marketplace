package com.ekko.seller_service.exception;

import java.util.UUID;

public class SellerPendingException extends SellerServiceException {

    public SellerPendingException(UUID sellerId) {
        super("Seller account is pending approval: " + sellerId);
    }
}
