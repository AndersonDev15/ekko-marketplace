package com.ekko.payment_service.application.exception;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {

    private final String paymentIntentId;
    private final UUID paymentId;

    public PaymentNotFoundException(String paymentIntentId) {
        super("Payment not found for payment intent " + paymentIntentId);
        this.paymentIntentId = paymentIntentId;
        this.paymentId = null;
    }

    public PaymentNotFoundException(UUID paymentId) {
        super("Payment not found for id: " + paymentId);
        this.paymentIntentId = null;
        this.paymentId = paymentId;
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }
}