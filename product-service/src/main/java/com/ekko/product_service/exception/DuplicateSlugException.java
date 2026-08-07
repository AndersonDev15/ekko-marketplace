package com.ekko.product_service.exception;

public class DuplicateSlugException extends RuntimeException {

    public DuplicateSlugException() {
        super("Category slug already exists");
    }

    public DuplicateSlugException(String message) {
        super(message);
    }
}