package com.ekko.product_service.messaging.dto.consume;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerSlugChangedEvent(
        UUID sellerId,
        String keycloakId,
        String slug,
        LocalDateTime changedAt
) {
}