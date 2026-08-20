package com.ekko.product_service.messaging.dto.consume;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consumed from review-service's review.created event. Product-service uses the
 * productId and rating to update the product's stored average rating and count.
 */
public record ReviewCreatedEvent(
        UUID reviewId,
        UUID productId,
        UUID orderId,
        UUID orderItemId,
        UUID sellerKeycloakId,
        String customerId,
        Integer rating,
        String title,
        String comment,
        LocalDateTime createdAt
) {
}