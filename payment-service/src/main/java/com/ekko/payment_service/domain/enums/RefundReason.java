package com.ekko.payment_service.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Reason for refund")
public enum RefundReason {
    @Schema(description = "Duplicate charge")
    DUPLICATE,
    @Schema(description = "Fraudulent transaction")
    FRAUDULENT,
    @Schema(description = "Customer requested refund")
    REQUESTED_BY_CUSTOMER
}