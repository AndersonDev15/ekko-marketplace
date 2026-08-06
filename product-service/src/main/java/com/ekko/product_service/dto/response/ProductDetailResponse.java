package com.ekko.product_service.dto.response;

import com.ekko.product_service.enums.ProductStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductDetailResponse(
        UUID id,
        UUID sellerKeycloakId,
        String name,
        String slug,
        String description,
        ProductStatus status,
        BigDecimal averageRating,
        Integer reviewCount,
        List<ProductAttributeResponse> attributes,
        List<ProductImageResponse> images,
        List<ProductVariantResponse> variants
) {
}