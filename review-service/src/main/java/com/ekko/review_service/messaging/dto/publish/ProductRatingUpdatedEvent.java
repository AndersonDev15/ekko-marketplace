package com.ekko.review_service.messaging.dto.publish;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRatingUpdatedEvent(
        UUID productId,
        BigDecimal averageRating,
        long reviewCount
) {
}