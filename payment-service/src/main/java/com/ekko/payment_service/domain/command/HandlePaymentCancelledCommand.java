package com.ekko.payment_service.domain.command;

public record HandlePaymentCancelledCommand(
        String paymentIntentId,
        String stripeEventId
) {
}