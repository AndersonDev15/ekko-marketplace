package com.ekko.notification_service.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error response")
public record ErrorResponse(
        @Schema(description = "Error code", example = "NOTIFICATION_NOT_FOUND")
        String error,

        @Schema(description = "Error message", example = "Notification not found with id: 123e4567-e89b-12d3-a456-426614174000")
        String message,

        @Schema(description = "HTTP status code", example = "404")
        Integer httpStatus
) {}