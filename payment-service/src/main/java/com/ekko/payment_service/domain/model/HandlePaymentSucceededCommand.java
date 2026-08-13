package com.ekko.payment_service.domain.model;

import java.time.LocalDateTime;

public record HandlePaymentSucceededCommand(
        String paymentIntentId,
        String stripeEventId,
        String paymentMethodType,
        String paymentMethodLast4,
        LocalDateTime paidAt
) {
}