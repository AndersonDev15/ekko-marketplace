package com.ekko.seller_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerAddressResponse(
        @Schema(description = "Address UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Street address", example = "Calle 123 #45-67")
        String addressLine,

        @Schema(description = "City", example = "Bogotá")
        String city,

        @Schema(description = "State/Department", example = "Cundinamarca")
        String state,

        @Schema(description = "Country", example = "Colombia")
        String country,

        @Schema(description = "Postal code", example = "110111")
        String postalCode,

        @Schema(description = "Whether this is the primary address", example = "true")
        Boolean isPrimary,

        @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
        LocalDateTime createdAt
) {}
