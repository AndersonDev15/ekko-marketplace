package com.ekko.payment_service.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentRefundedEvent(
        UUID paymentId,
        UUID orderId,
        UUID customerId,
        UUID refundId,
        BigDecimal amount,
        boolean isTotal,
        LocalDateTime refundedAt
) {
}