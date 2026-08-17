package com.ekko.order_service.application.exception;

public class OrderNumberGenerationException extends RuntimeException {

    public OrderNumberGenerationException(String message) {
        super(message);
    }
}