package com.ekko.payment_service.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentCompletedEvent(
        UUID paymentId,
        UUID orderId,
        UUID customerId,
        BigDecimal amount,
        String currency,
        LocalDateTime completedAt
) {
}