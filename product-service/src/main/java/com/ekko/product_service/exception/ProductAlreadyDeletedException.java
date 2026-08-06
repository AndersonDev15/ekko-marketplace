package com.ekko.product_service.exception;

public class ProductAlreadyDeletedException extends RuntimeException {

    public ProductAlreadyDeletedException() {
        super("Product is already deleted");
    }

    public ProductAlreadyDeletedException(String message) {
        super(message);
    }
}