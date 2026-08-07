package com.ekko.product_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record VariantSummaryResponse(
        UUID id,
        UUID productId,
        String sku,
        BigDecimal price,
        BigDecimal discountPrice,
        String currency,
        boolean isActive,
        List<VariantAttributeResponse> attributes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}