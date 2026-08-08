package com.ekko.product_service.exception;

public class InvalidPriceRangeException extends RuntimeException {

    public InvalidPriceRangeException() {
        super("Invalid price range: minPrice must be less than or equal to maxPrice");
    }

    public InvalidPriceRangeException(String message) {
        super(message);
    }
}