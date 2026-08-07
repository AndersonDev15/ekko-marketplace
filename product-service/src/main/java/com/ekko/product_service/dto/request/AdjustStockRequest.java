package com.ekko.product_service.dto.request;

import java.util.UUID;

public record AdjustStockRequest(
        Long newStock
) {
}