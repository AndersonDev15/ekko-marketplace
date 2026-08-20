package com.ekko.product_service.messaging.dto.publish;

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