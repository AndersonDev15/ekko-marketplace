package com.ekko.order_service.application.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderNumberOrId) {
        super("Order not found: " + orderNumberOrId);
    }
}