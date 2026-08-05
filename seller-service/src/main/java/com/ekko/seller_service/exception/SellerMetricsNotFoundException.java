package com.ekko.seller_service.exception;

import java.util.UUID;

public class SellerMetricsNotFoundException extends SellerServiceException {

    public SellerMetricsNotFoundException(UUID sellerId) {
        super("Seller metrics not found for seller: " + sellerId);
    }
}
