package com.ekko.seller_service.messaging.dto.consume;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consumed from review-service's review.created event.
 * Mirrors the fields published by review-service; the sellerKeycloakId is what
 * allows attributing the review to the seller metrics.
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