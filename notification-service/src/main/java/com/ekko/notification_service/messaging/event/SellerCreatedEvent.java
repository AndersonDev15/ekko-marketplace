package com.ekko.notification_service.messaging.event;

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