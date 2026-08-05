package com.ekko.seller_service.dto.response;

import com.ekko.seller_service.enums.SellerStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SellerDetailResponse(
        UUID id,
        String keycloakId,
        String storeName,
        String email,
        String phone,
        String description,
        String logoUrl,
        SellerStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<SellerDocumentResponse> documents,
        SellerMetricsResponse metrics
) {}
