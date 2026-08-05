package com.ekko.seller_service.dto.response;

import com.ekko.seller_service.enums.SellerStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerResponse(
        UUID id,
        String keycloakId,
        String storeName,
        String email,
        String phone,
        String description,
        String logoUrl,
        SellerStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
