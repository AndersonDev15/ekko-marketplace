package com.ekko.product_service.exception;

public class NoActiveVariantException extends RuntimeException {

    public NoActiveVariantException() {
        super("Product must have at least one active variant");
    }

    public NoActiveVariantException(String message) {
        super(message);
    }
}