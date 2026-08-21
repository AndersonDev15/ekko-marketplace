package com.ekko.notification_service.messaging.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProductDeactivatedEvent(
        UUID productId,
        UUID sellerKeycloakId,
        ProductStatus previousStatus,
        LocalDateTime deactivatedAt
) {
}