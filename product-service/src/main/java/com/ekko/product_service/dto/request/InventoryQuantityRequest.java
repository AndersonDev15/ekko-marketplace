package com.ekko.product_service.dto.request;

import java.util.UUID;

public record InventoryQuantityRequest(
        UUID variantId,
        long quantity
) {
}