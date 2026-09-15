package com.ekko.product_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Product variant info for internal service communication")
public record ProductVariantInfo(
        @Schema(description = "Variant ID", example = "123e4567-e89b-12d3-a456-426614174006")
        UUID variantId,

        @Schema(description = "Product ID", example = "123e4567-e89b-12d3-a456-426614174005")
        UUID productId,

        @Schema(description = "Product name", example = "Awesome Product")
        String productName,

        @Schema(description = "Variant SKU", example = "PROD-001-RED-M")
        String sku,

        @Schema(description = "Variant price", example = "29.99")
        BigDecimal price,

        @Schema(description = "Seller Keycloak ID", example = "123e4567-e89b-12d3-a456-426614174004")
        UUID sellerKeycloakId,

        @Schema(description = "Store name", example = "My Store")
        String storeName,

        @Schema(description = "Product image URL", example = "https://example.com/image.jpg")
        String imageUrl,

        @Schema(description = "Available stock", example = "50")
        Long availableStock
) {
}
