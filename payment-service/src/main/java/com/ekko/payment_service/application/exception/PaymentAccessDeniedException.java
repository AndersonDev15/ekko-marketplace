package com.ekko.payment_service.application.exception;

public class PaymentAccessDeniedException extends RuntimeException {

    public PaymentAccessDeniedException() {
        super("Access denied to this payment");
    }
}