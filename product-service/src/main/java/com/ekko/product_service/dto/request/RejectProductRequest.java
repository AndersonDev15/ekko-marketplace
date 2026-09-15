package com.ekko.product_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to reject a product")
public record RejectProductRequest(
        @Schema(description = "Rejection reason", example = "Product description is incomplete", requiredMode = Schema.RequiredMode.REQUIRED)
        String reason
) {
}