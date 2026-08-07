package com.ekko.product_service.exception;

public class InactiveParentCategoryException extends RuntimeException {

    public InactiveParentCategoryException() {
        super("Parent category is inactive");
    }

    public InactiveParentCategoryException(String message) {
        super(message);
    }
}