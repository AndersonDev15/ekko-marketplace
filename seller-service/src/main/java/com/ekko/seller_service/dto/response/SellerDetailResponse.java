package com.ekko.seller_service.dto.response;

import com.ekko.seller_service.enums.SellerStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SellerDetailResponse(
        @Schema(description = "Seller UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Keycloak user ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String keycloakId,

        @Schema(description = "Store name", example = "My Store")
        String storeName,

        @Schema(description = "Seller email", example = "seller@example.com")
        String email,

        @Schema(description = "Phone number", example = "+573001234567")
        String phone,

        @Schema(description = "Store description", example = "Best store in town")
        String description,

        @Schema(description = "Logo URL", example = "https://cdn.example.com/logos/store123.png")
        String logoUrl,

        @Schema(description = "Seller status", example = "ACTIVE")
        SellerStatus status,

        @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
        LocalDateTime createdAt,

        @Schema(description = "Last update timestamp", example = "2024-01-20T14:45:00")
        LocalDateTime updatedAt,

        @Schema(description = "Seller documents")
        List<SellerDocumentResponse> documents,

        @Schema(description = "Seller metrics")
        SellerMetricsResponse metrics
) {}
