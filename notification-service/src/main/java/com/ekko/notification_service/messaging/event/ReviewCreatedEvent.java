package com.ekko.notification_service.messaging.event;

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