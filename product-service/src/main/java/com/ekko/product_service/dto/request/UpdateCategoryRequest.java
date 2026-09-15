package com.ekko.product_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to update an existing category")
public record UpdateCategoryRequest(
        @Schema(description = "Category name", example = "Electronics Updated")
        String name,

        @Schema(description = "Category description", example = "Updated electronic devices and accessories")
        String description
) {
}