package com.ekko.product_service.messaging.dto.publish;

import com.ekko.product_service.enums.ProductStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProductDeactivatedEvent(
        UUID productId,
        UUID sellerKeycloakId,
        ProductStatus previousStatus,
        LocalDateTime deactivatedAt
) {
}