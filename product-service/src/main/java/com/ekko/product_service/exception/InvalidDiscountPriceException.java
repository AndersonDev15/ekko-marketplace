package com.ekko.product_service.exception;

public class InvalidDiscountPriceException extends RuntimeException {

    public InvalidDiscountPriceException() {
        super("Discount price must be lower than price");
    }

    public InvalidDiscountPriceException(String message) {
        super(message);
    }
}
