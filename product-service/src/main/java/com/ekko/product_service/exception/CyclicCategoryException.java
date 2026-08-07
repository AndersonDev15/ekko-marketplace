package com.ekko.product_service.exception;

public class CyclicCategoryException extends RuntimeException {

    public CyclicCategoryException() {
        super("Category hierarchy would create a cycle");
    }

    public CyclicCategoryException(String message) {
        super(message);
    }
}