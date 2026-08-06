package com.ekko.product_service.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductVariantResponse(
        UUID id,
        String sku,
        BigDecimal price,
        BigDecimal discountPrice,
        String currency,
        Boolean isActive,
        InventoryResponse inventory,
        List<ProductVariantAttributeResponse> attributes
) {
}