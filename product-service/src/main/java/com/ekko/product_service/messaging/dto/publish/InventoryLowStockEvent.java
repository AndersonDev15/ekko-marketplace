package com.ekko.product_service.messaging.dto.publish;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryLowStockEvent(
        UUID productId,
        UUID variantId,
        UUID sellerKeycloakId,
        long currentStock,
        long minimumStock,
        LocalDateTime checkedAt
) {
}