package com.ekko.order_service.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentData(
        UUID paymentId,
        BigDecimal amount,
        String currency,
        LocalDateTime paidAt
) {
}