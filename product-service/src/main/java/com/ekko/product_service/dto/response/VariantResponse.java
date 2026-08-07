package com.ekko.product_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record VariantResponse(
        UUID id,
        UUID productId,
        String sku,
        BigDecimal price,
        BigDecimal discountPrice,
        String currency,
        boolean isActive,
        long stockAvailable,
        List<VariantAttributeResponse> attributes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}