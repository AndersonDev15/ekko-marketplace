package com.ekko.product_service.exception;

public class ProductNotAvailableException extends RuntimeException {

    public ProductNotAvailableException() {
        super("Product is not available for purchase");
    }

    public ProductNotAvailableException(String message) {
        super(message);
    }
}