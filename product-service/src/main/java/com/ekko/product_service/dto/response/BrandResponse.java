package com.ekko.product_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Brand response")
public record BrandResponse(
        @Schema(description = "Brand ID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Brand name", example = "Nike")
        String name,

        @Schema(description = "Brand slug", example = "nike")
        String slug,

        @Schema(description = "Brand logo URL", example = "https://example.com/logo.png")
        String logoUrl,

        @Schema(description = "Brand description", example = "Sports apparel and footwear")
        String description,

        @Schema(description = "Whether brand is active", example = "true")
        Boolean isActive
) {
}