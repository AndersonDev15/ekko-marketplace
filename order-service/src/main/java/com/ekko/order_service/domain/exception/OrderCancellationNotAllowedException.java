package com.ekko.order_service.domain.exception;

import com.ekko.order_service.domain.enums.OrderStatus;

public class OrderCancellationNotAllowedException extends RuntimeException {

    public OrderCancellationNotAllowedException(OrderStatus currentStatus) {
        super("Order cannot be cancelled in status: " + currentStatus);
    }
}