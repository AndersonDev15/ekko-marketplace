package com.ekko.payment_service.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Transfer status")
public enum TransferStatus {
    @Schema(description = "Transfer initiated, awaiting processing")
    PENDING,
    @Schema(description = "Transfer completed successfully")
    SUCCEEDED,
    @Schema(description = "Transfer failed")
    FAILED
}