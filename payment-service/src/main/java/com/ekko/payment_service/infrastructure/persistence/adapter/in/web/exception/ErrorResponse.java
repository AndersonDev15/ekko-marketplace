package com.ekko.payment_service.infrastructure.persistence.adapter.in.web.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Error response")
public record ErrorResponse(
        @Schema(description = "Error code", example = "PAYMENT_NOT_FOUND")
        String error,

        @Schema(description = "Human-readable error message", example = "Payment not found: a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String message,

        @Schema(description = "Vendor UUID (when applicable)", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
        UUID variantId,

        @Schema(description = "HTTP status code", example = "404")
        Integer httpStatus
) {
}