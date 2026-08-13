package com.ekko.payment_service.domain.model;

public record HandlePaymentCancelledCommand(
        String paymentIntentId,
        String stripeEventId
) {
}