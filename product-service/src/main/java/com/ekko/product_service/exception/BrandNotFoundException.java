package com.ekko.product_service.exception;

public class BrandNotFoundException extends RuntimeException {

    public BrandNotFoundException() {
        super("Brand not found");
    }

    public BrandNotFoundException(String message) {
        super(message);
    }
}