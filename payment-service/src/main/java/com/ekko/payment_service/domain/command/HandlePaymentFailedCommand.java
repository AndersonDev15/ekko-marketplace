package com.ekko.payment_service.domain.command;

import com.ekko.payment_service.domain.enums.PaymentFailureReason;

public record HandlePaymentFailedCommand(
        String paymentIntentId,
        String stripeEventId,
        PaymentFailureReason reason,
        String errorMessage
) {
}