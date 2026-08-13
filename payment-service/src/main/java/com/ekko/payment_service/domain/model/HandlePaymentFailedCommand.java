package com.ekko.payment_service.domain.model;

public record HandlePaymentFailedCommand(
        String paymentIntentId,
        String stripeEventId,
        PaymentFailureReason reason,
        String errorMessage
) {
}