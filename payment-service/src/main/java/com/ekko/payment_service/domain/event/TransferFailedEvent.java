package com.ekko.payment_service.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransferFailedEvent(
        UUID transferId,
        UUID paymentId,
        UUID orderId,
        UUID vendorId,
        BigDecimal amount,
        String currency,
        LocalDateTime failedAt
) {
}