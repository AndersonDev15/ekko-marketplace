package com.ekko.notification_service.messaging.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProductPublishedEvent(
        UUID productId,
        UUID sellerKeycloakId,
        String name,
        String category,
        LocalDateTime publishedAt
) {
}