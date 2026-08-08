package com.ekko.order_service.domain.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderNumberOrId) {
        super("Order not found: " + orderNumberOrId);
    }
}