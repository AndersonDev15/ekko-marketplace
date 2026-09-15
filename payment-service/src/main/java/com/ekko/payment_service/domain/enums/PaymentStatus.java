package com.ekko.payment_service.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payment status")
public enum PaymentStatus {
    @Schema(description = "Payment initiated, awaiting processing")
    PENDING,
    @Schema(description = "Payment being processed")
    PROCESSING,
    @Schema(description = "Payment completed successfully")
    SUCCEEDED,
    @Schema(description = "Payment failed")
    FAILED,
    @Schema(description = "Payment cancelled")
    CANCELLED,
    @Schema(description = "Payment refunded")
    REFUNDED,
    @Schema(description = "Payment disputed")
    DISPUTED
}
