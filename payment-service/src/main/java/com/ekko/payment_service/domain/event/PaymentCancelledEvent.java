package com.ekko.payment_service.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentCancelledEvent(
        UUID paymentId,
        UUID orderId,
        UUID customerId,
        LocalDateTime cancelledAt
) {
}