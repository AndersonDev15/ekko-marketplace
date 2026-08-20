package com.ekko.seller_service.messaging.dto.consume;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consumed from product-service's product.deactivated event.
 * sellerId is the seller's Keycloak UUID. previousStatus is informational only.
 */
public record ProductDeactivatedEvent(
        UUID productId,
        UUID sellerKeycloakId,
        String previousStatus,
        LocalDateTime deactivatedAt
) {
}