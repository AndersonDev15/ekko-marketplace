package com.ekko.product_service.exception;

public class ForbiddenProductAccessException extends RuntimeException {

    public ForbiddenProductAccessException() {
        super("Forbidden product access");
    }

    public ForbiddenProductAccessException(String message) {
        super(message);
    }
}