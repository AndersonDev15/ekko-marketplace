package com.ekko.product_service.messaging.dto;

import com.ekko.product_service.enums.ProductStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProductDeactivatedEvent(
        UUID productId,
        UUID sellerId,
        ProductStatus previousStatus,
        LocalDateTime deactivatedAt
) {
}