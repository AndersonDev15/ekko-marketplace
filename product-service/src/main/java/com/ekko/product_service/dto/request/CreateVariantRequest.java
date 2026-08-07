package com.ekko.product_service.dto.request;

import java.math.BigDecimal;
import java.util.List;

public record CreateVariantRequest(
        String sku,
        BigDecimal price,
        BigDecimal discountPrice,
        String currency,
        List<CreateVariantAttributeRequest> attributes
) {
}