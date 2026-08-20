package com.ekko.seller_service.messaging.dto.publish;

import com.ekko.seller_service.enums.SellerStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerCreatedEvent(
        UUID sellerId,
        String keycloakId,
        String storeName,
        String email,
        SellerStatus status,
        LocalDateTime createdAt
) {
}