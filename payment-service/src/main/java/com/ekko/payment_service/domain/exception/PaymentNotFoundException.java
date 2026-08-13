package com.ekko.payment_service.domain.exception;

public class PaymentNotFoundException extends RuntimeException {

    private final String paymentIntentId;

    public PaymentNotFoundException(String paymentIntentId) {
        super("Payment not found for payment intent " + paymentIntentId);
        this.paymentIntentId = paymentIntentId;
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }
}