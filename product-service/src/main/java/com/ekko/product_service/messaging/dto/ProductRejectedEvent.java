package com.ekko.product_service.messaging.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProductRejectedEvent(
        UUID productId,
        UUID sellerId,
        String name,
        String reason,
        LocalDateTime rejectedAt
) {
}