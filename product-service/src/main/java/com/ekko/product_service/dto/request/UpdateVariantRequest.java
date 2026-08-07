package com.ekko.product_service.dto.request;

import java.math.BigDecimal;

public record UpdateVariantRequest(
        String sku,
        BigDecimal price,
        BigDecimal discountPrice
) {
}