package com.ekko.review_service.messaging.dto;

import java.time.LocalDateTime;
import java.util.UUID;

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