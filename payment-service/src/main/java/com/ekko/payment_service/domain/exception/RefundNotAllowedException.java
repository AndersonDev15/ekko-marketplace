package com.ekko.payment_service.domain.exception;

import com.ekko.payment_service.domain.model.PaymentStatus;

import java.util.UUID;

public class RefundNotAllowedException extends RuntimeException {

    private final UUID paymentId;
    private final PaymentStatus currentStatus;

    public RefundNotAllowedException(UUID paymentId, PaymentStatus currentStatus) {
        super("Refund not allowed for payment " + paymentId + " because its status is " + currentStatus
                + "; only SUCCEEDED payments can be refunded");
        this.paymentId = paymentId;
        this.currentStatus = currentStatus;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public PaymentStatus getCurrentStatus() {
        return currentStatus;
    }
}