package com.ekko.order_service.domain.model;

import java.util.UUID;

public record StockItem(
        UUID variantId,
        long quantity
) {
}