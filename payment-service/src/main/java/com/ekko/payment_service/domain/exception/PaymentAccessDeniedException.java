package com.ekko.payment_service.domain.exception;

public class PaymentAccessDeniedException extends RuntimeException {

    public PaymentAccessDeniedException() {
        super("Access denied to this payment");
    }
}