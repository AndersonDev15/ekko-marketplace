package com.ekko.seller_service.exception;

import java.util.UUID;

public class SellerAddressNotFoundException extends SellerServiceException {

    public SellerAddressNotFoundException(UUID addressId) {
        super("Seller address not found with id: " + addressId);
    }

    public SellerAddressNotFoundException(UUID addressId, UUID sellerId) {
        super("Seller address " + addressId + " not found for seller: " + sellerId);
    }
}