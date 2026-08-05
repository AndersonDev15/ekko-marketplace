package com.ekko.seller_service.exception;

import java.util.UUID;

public class SellerSuspendedException extends SellerServiceException {

    public SellerSuspendedException(UUID sellerId) {
        super("Seller account is suspended: " + sellerId);
    }
}
