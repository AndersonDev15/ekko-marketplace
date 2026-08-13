package com.ekko.payment_service.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateRefundCommand(
        UUID paymentId,
        BigDecimal amount,
        RefundReason reason,
        String notes,
        UUID requestedBy
) {
}