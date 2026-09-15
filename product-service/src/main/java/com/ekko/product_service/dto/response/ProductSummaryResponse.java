package com.ekko.product_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Product summary for catalog listings")
public record ProductSummaryResponse(
        @Schema(description = "Product ID", example = "123e4567-e89b-12d3-a456-426614174005")
        UUID id,

        @Schema(description = "Product name", example = "Awesome Product")
        String name,

        @Schema(description = "Product slug", example = "awesome-product")
        String slug,

        @Schema(description = "Seller slug", example = "my-store")
        String sellerSlug,

        @Schema(description = "Product price", example = "29.99")
        BigDecimal price,

        @Schema(description = "Main image URL", example = "https://example.com/image.jpg")
        String mainImageUrl,

        @Schema(description = "Brand name", example = "Nike")
        String brandName,

        @Schema(description = "Category name", example = "Electronics")
        String categoryName,

        @Schema(description = "Whether product has stock", example = "true")
        boolean hasStock
) {
}