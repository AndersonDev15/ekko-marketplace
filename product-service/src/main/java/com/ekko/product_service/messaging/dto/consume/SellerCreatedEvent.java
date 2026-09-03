package com.ekko.product_service.messaging.dto.consume;

import com.ekko.product_service.enums.SellerStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerCreatedEvent(
        UUID sellerId,
        String keycloakId,
        String storeName,
        String slug,
        String email,
        SellerStatus status,
        LocalDateTime createdAt
) {}
