package com.ekko.payment_service.domain.event;

import com.ekko.payment_service.domain.enums.PaymentFailureReason;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID paymentId,
        UUID orderId,
        UUID customerId,
        String guestEmail,
        PaymentFailureReason reason,
        LocalDateTime failedAt
) {
}