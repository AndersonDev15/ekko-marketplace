package com.ekko.order_service.application.exception;

public class OrderAccessDeniedException extends RuntimeException {

    public OrderAccessDeniedException() {
        super("Access denied to this order");
    }
}