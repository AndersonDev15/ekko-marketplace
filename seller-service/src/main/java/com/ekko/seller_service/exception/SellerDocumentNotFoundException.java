package com.ekko.seller_service.exception;

import java.util.UUID;

public class SellerDocumentNotFoundException extends SellerServiceException {

    public SellerDocumentNotFoundException(UUID documentId) {
        super("Seller document not found with id: " + documentId);
    }

    public SellerDocumentNotFoundException(UUID documentId, UUID sellerId) {
        super("Seller document " + documentId + " not found for seller: " + sellerId);
    }
}
