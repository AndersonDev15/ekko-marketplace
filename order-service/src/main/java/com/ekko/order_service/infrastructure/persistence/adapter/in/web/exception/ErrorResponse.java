package com.ekko.order_service.infrastructure.persistence.adapter.in.web.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Error response format")
public record ErrorResponse(
        @Schema(description = "Error code", example = "ORDER_NOT_FOUND")
        String error,

        @Schema(description = "Human-readable error message", example = "Order not found: ORD-20240115-ABC123")
        String message,

        @Schema(description = "Product variant ID (for stock-related errors)")
        UUID variantId,

        @Schema(description = "HTTP status code", example = "404")
        Integer httpStatus
) {
}