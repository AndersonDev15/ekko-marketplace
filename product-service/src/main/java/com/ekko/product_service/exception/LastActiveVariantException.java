package com.ekko.product_service.exception;

public class LastActiveVariantException extends RuntimeException {

    public LastActiveVariantException() {
        super("Cannot deactivate the last active variant of the product");
    }

    public LastActiveVariantException(String message) {
        super(message);
    }
}
