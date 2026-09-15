package com.ekko.review_service.exception;

import io.swagger.v3.oas.annotations.media.Schema;

public record ErrorResponse(
        @Schema(description = "Error code", example = "REVIEW_NOT_FOUND")
        String error,

        @Schema(description = "Error message", example = "Review not found")
        String message,

        @Schema(description = "HTTP status code", example = "404")
        Integer httpStatus
) {
}