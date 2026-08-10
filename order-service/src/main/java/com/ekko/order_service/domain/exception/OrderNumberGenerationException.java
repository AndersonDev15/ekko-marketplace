package com.ekko.order_service.domain.exception;

public class OrderNumberGenerationException extends RuntimeException {

    public OrderNumberGenerationException(String message) {
        super(message);
    }
}