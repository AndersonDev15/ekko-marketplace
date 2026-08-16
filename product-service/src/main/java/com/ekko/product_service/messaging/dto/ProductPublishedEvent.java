package com.ekko.product_service.messaging.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProductPublishedEvent(
        UUID productId,
        UUID sellerId,
        String name,
        String category,
        LocalDateTime publishedAt
) {
}