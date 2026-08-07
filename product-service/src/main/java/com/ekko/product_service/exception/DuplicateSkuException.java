package com.ekko.product_service.exception;

public class DuplicateSkuException extends RuntimeException {

    public DuplicateSkuException() {
        super("Variant SKU already exists");
    }

    public DuplicateSkuException(String message) {
        super(message);
    }
}
