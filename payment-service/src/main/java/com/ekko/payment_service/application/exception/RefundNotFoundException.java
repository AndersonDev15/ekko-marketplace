package com.ekko.payment_service.application.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class RefundNotFoundException extends RuntimeException {

    public RefundNotFoundException(String stripeRefundId) {
        super("Refund not found for Stripe refund id " + stripeRefundId);
    }

    public RefundNotFoundException(UUID refundId) {
        super("Refund not found: " + refundId);
    }

}