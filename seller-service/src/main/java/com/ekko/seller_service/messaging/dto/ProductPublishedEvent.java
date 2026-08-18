package com.ekko.seller_service.messaging.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consumed from product-service's product.published event.
 * sellerId is the seller's Keycloak UUID (product-service stores the seller by
 * keycloak id), used to attribute the product to the seller metrics.
 */
public record ProductPublishedEvent(
        UUID productId,
        UUID sellerId,
        String name,
        String category,
        LocalDateTime publishedAt
) {
}