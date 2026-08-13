package com.ekko.payment_service.domain.exception;

import java.util.UUID;

public class PaymentAlreadySucceededException extends RuntimeException {

    private final UUID orderId;

    public PaymentAlreadySucceededException(UUID orderId) {
        super("Payment already succeeded for order " + orderId);
        this.orderId = orderId;
    }

    public UUID getOrderId() {
        return orderId;
    }
}
