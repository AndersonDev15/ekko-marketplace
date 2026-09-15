package com.ekko.product_service.dto.response;

import com.ekko.product_service.enums.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Detailed product response for public catalog")
public record ProductDetailResponse(
        @Schema(description = "Product ID", example = "123e4567-e89b-12d3-a456-426614174005")
        UUID id,

        @Schema(description = "Seller Keycloak ID", example = "123e4567-e89b-12d3-a456-426614174004")
        UUID sellerKeycloakId,

        @Schema(description = "Product name", example = "Awesome Product")
        String name,

        @Schema(description = "Product slug", example = "awesome-product")
        String slug,

        @Schema(description = "Product description", example = "This is an awesome product")
        String description,

        @Schema(description = "Product status")
        ProductStatus status,

        @Schema(description = "Average rating", example = "4.5")
        BigDecimal averageRating,

        @Schema(description = "Review count", example = "10")
        Integer reviewCount,

        @Schema(description = "Product attributes")
        List<ProductAttributeResponse> attributes,

        @Schema(description = "Product images")
        List<ProductImageResponse> images,

        @Schema(description = "Product variants")
        List<ProductVariantResponse> variants
) {
}