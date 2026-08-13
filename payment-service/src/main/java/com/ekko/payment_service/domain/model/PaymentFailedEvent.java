package com.ekko.payment_service.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID paymentId,
        UUID orderId,
        UUID customerId,
        PaymentFailureReason reason,
        LocalDateTime failedAt
) {
}