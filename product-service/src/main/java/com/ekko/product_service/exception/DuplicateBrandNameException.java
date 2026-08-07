package com.ekko.product_service.exception;

public class DuplicateBrandNameException extends RuntimeException {

    public DuplicateBrandNameException() {
        super("Brand name already exists");
    }

    public DuplicateBrandNameException(String message) {
        super(message);
    }
}