package com.ekko.product_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Request to update an existing product")
public record UpdateProductRequest(
        @Schema(description = "Product name", example = "Updated Product Name")
        String name,

        @Schema(description = "Product description", example = "Updated product description")
        String description,

        @Schema(description = "Brand ID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID brandId,

        @Schema(description = "Category ID", example = "123e4567-e89b-12d3-a456-426614174001")
        UUID categoryId
) {
}