package com.ekko.seller_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        @Schema(description = "Error timestamp", example = "2024-01-15T10:30:00Z")
        Instant timestamp,

        @Schema(description = "HTTP status code", example = "404")
        int status,

        @Schema(description = "Error name", example = "Not Found")
        String error,

        @Schema(description = "Error message", example = "Seller not found")
        String message,

        @Schema(description = "Request path", example = "/sellers/550e8400-e29b-41d4-a716-446655440000")
        String path,

        @Schema(description = "Field validation errors (only for 400 errors)")
        List<FieldError> fieldErrors
) {
    @Schema(description = "Field validation error")
    public record FieldError(
            @Schema(description = "Field name", example = "storeName")
            String field,

            @Schema(description = "Error message", example = "El nombre de la tienda es obligatorio")
            String message
    ) {}

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(),
                status,
                error,
                message,
                path,
                null);
    }

    public static ErrorResponse of(int status, String error, String message, String path, List<FieldError> fieldErrors) {
        return new ErrorResponse(Instant.now(),
                status,
                error,
                message,
                path,
                fieldErrors);
    }
}
