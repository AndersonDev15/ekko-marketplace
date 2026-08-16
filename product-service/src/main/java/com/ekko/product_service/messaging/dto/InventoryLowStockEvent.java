package com.ekko.product_service.messaging.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryLowStockEvent(
        UUID productId,
        UUID variantId,
        UUID sellerId,
        long currentStock,
        long minimumStock,
        LocalDateTime checkedAt
) {
}