package com.ekko.order_service.domain.exception;

public class OrderAccessDeniedException extends RuntimeException {

    public OrderAccessDeniedException() {
        super("Access denied to this order");
    }
}