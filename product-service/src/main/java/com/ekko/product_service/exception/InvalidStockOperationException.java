package com.ekko.product_service.exception;

public class InvalidStockOperationException extends RuntimeException {

    public InvalidStockOperationException() {
        super("Invalid stock operation");
    }

    public InvalidStockOperationException(String message) {
        super(message);
    }
}