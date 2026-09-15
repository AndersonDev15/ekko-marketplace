package com.ekko.product_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Product catalog response for public listing")
public record ProductCatalogResponse(
        @Schema(description = "Product ID", example = "123e4567-e89b-12d3-a456-426614174005")
        UUID id,

        @Schema(description = "Product name", example = "Awesome Product")
        String name,

        @Schema(description = "Product slug", example = "awesome-product")
        String slug,

        @Schema(description = "Seller slug", example = "my-store")
        String sellerSlug,

        @Schema(description = "Product description", example = "This is an awesome product")
        String description,

        @Schema(description = "Average rating", example = "4.5")
        BigDecimal averageRating,

        @Schema(description = "Review count", example = "10")
        Integer reviewCount,

        @Schema(description = "Primary image URL", example = "https://example.com/image.jpg")
        String primaryImageUrl
) {
}