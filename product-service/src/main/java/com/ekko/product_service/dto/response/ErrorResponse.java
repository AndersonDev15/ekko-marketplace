package com.ekko.product_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Error response")
public record ErrorResponse(
        @Schema(description = "Error code", example = "INSUFFICIENT_STOCK")
        String error,

        @Schema(description = "Error message", example = "Insufficient stock for variant")
        String message,

        @Schema(description = "Variant ID (if applicable)", example = "123e4567-e89b-12d3-a456-426614174006")
        UUID variantId,

        @Schema(description = "HTTP status code", example = "409")
        Integer httpStatus
) {
}