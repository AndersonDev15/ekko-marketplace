package com.ekko.product_service.messaging.dto.publish;

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