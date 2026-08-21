package com.ekko.notification_service.messaging.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProductRejectedEvent(
        UUID productId,
        UUID sellerKeycloakId,
        String name,
        String reason,
        LocalDateTime rejectedAt
) {
}