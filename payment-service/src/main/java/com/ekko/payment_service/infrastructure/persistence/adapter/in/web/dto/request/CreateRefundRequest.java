package com.ekko.payment_service.infrastructure.persistence.adapter.in.web.dto.request;

import com.ekko.payment_service.domain.enums.RefundReason;
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