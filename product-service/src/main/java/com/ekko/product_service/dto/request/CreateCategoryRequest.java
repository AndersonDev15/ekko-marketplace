package com.ekko.product_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Request to create a new category")
public record CreateCategoryRequest(
        @Schema(description = "Category name", example = "Electronics", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "Category description", example = "Electronic devices and accessories")
        String description,

        @Schema(description = "Parent category ID (null for root category)", example = "123e4567-e89b-12d3-a456-426614174002")
        UUID parentId
) {
}