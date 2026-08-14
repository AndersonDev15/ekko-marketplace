package com.ekko.payment_service.infrastructure.web.dto;

import com.ekko.payment_service.domain.model.RefundReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateRefundRequest(
        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        BigDecimal amount,

        @NotNull(message = "reason is required")
        RefundReason reason,

        String notes
) {
}