package com.ekko.seller_service.messaging.dto.publish;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerSlugChangedEvent(
        UUID sellerId,
        String keycloakId,
        String slug,
        LocalDateTime changedAt
) {
}
