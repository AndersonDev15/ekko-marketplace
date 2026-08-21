package com.ekko.notification_service.messaging.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID paymentId,
        UUID orderId,
        UUID customerId,
        String customerEmail,
        PaymentFailureReason reason,
        LocalDateTime failedAt
) {
}