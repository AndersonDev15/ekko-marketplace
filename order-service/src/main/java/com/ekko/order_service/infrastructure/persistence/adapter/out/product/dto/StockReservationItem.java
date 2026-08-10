package com.ekko.order_service.infrastructure.persistence.adapter.out.product.dto;

import java.util.UUID;

public record StockReservationItem(
        UUID variantId,
        long quantity
) {
}