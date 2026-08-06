package com.ekko.product_service.exception;

public class NoPrimaryImageException extends RuntimeException {

    public NoPrimaryImageException() {
        super("Product must have a primary image");
    }

    public NoPrimaryImageException(String message) {
        super(message);
    }
}