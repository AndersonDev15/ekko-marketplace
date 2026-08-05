package com.ekko.seller_service.exception;

public abstract class SellerServiceException extends RuntimeException {

    protected SellerServiceException(String message) {
        super(message);
    }

    protected SellerServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

