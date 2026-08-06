package com.ekko.product_service.exception;

public class InvalidProductStatusException extends RuntimeException {

    public InvalidProductStatusException() {
        super("Invalid product status");
    }

    public InvalidProductStatusException(String message) {
        super(message);
    }
}