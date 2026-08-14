package com.ekko.payment_service.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentRefundFailedEvent(
        UUID paymentId,
        UUID orderId,
        UUID refundId,
        LocalDateTime failedAt
) {
}