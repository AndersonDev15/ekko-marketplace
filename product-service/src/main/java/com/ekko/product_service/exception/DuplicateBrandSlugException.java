package com.ekko.product_service.exception;

public class DuplicateBrandSlugException extends RuntimeException {

    public DuplicateBrandSlugException() {
        super("Brand slug already exists");
    }

    public DuplicateBrandSlugException(String message) {
        super(message);
    }
}