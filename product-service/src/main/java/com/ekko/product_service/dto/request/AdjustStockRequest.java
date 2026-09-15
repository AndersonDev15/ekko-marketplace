package com.ekko.product_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to adjust stock quantity")
public record AdjustStockRequest(
        @Schema(description = "New stock quantity", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
        Long newStock
) {
}