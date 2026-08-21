package com.ekko.notification_service.messaging.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerStatusChangedEvent(
        UUID sellerId,
        String keycloakId,
        String email,
        SellerStatus previousStatus,
        SellerStatus newStatus,
        LocalDateTime changedAt
) {
}