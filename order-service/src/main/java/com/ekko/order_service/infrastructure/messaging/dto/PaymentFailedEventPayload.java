package com.ekko.order_service.infrastructure.messaging.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentFailedEventPayload(
        UUID paymentId,
        UUID orderId,
        UUID customerId,
        String reason,
        LocalDateTime failedAt
) {
}