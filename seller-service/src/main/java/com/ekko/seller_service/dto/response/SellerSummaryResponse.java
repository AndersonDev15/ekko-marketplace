package com.ekko.seller_service.dto.response;

import com.ekko.seller_service.enums.SellerStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerSummaryResponse(
        UUID id,
        String storeName,
        String email,
        SellerStatus status,
        LocalDateTime createdAt
) {}
