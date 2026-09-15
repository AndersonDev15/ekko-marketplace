package com.ekko.seller_service.dto.response;

import com.ekko.seller_service.enums.SellerStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerSummaryResponse(
        @Schema(description = "Seller UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Store name", example = "My Store")
        String storeName,

        @Schema(description = "Seller email", example = "seller@example.com")
        String email,

        @Schema(description = "Seller status", example = "ACTIVE")
        SellerStatus status,

        @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
        LocalDateTime createdAt
) {}
