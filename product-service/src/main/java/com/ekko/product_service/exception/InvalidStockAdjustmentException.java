package com.ekko.product_service.exception;

public class InvalidStockAdjustmentException extends RuntimeException {

    public InvalidStockAdjustmentException() {
        super("Invalid stock adjustment");
    }

    public InvalidStockAdjustmentException(String message) {
        super(message);
    }
}