package com.ekko.product_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Product image response")
public record ProductImageResponse(
        @Schema(description = "Image ID", example = "123e4567-e89b-12d3-a456-426614174007")
        UUID id,

        @Schema(description = "Image URL", example = "https://example.com/image.jpg")
        String url,

        @Schema(description = "Whether this is the primary image", example = "true")
        Boolean isPrimary,

        @Schema(description = "Sort order", example = "0")
        Integer sortOrder
) {
}