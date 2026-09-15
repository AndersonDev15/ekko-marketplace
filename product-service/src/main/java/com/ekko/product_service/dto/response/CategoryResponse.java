package com.ekko.product_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Category response for admin operations")
public record CategoryResponse(
        @Schema(description = "Category ID", example = "123e4567-e89b-12d3-a456-426614174001")
        UUID id,

        @Schema(description = "Category name", example = "Electronics")
        String name,

        @Schema(description = "Category slug", example = "electronics")
        String slug,

        @Schema(description = "Whether category is active", example = "true")
        Boolean isActive
) {
}