package com.ekko.product_service.exception;

public class VariantNotFoundException extends RuntimeException {

    public VariantNotFoundException() {
        super("Variant not found");
    }

    public VariantNotFoundException(String message) {
        super(message);
    }
}
