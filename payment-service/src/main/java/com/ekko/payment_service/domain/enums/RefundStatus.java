package com.ekko.payment_service.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Refund status")
public enum RefundStatus {
    @Schema(description = "Refund initiated, awaiting processing")
    PENDING,
    @Schema(description = "Refund completed successfully")
    SUCCEEDED,
    @Schema(description = "Refund failed")
    FAILED,
    @Schema(description = "Refund cancelled")
    CANCELLED,
    @Schema(description = "Refund rejected by admin")
    REJECTED
}