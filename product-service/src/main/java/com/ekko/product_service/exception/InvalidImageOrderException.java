package com.ekko.product_service.exception;

public class InvalidImageOrderException extends RuntimeException {

    public InvalidImageOrderException() {
        super("Invalid image order");
    }

    public InvalidImageOrderException(String message) {
        super(message);
    }
}
