package com.ekko.payment_service.infrastructure.persistence.adapter.in.web.dto.request;

import com.ekko.payment_service.domain.enums.RefundReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Request to create a refund")
public record CreateRefundRequest(
        @Schema(description = "Refund amount", example = "50.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        BigDecimal amount,

        @Schema(description = "Reason for refund", example = "DUPLICATE", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "reason is required")
        RefundReason reason,

        @Schema(description = "Optional notes about the refund", example = "Customer requested refund")
        String notes
) {
}