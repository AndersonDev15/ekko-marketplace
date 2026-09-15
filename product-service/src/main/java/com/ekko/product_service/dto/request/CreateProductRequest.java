package com.ekko.product_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Request to create a new product")
public record CreateProductRequest(
        @Schema(description = "Product name", example = "Awesome Product", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "Product description", example = "This is an awesome product", requiredMode = Schema.RequiredMode.REQUIRED)
        String description,

        @Schema(description = "Brand ID", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID brandId,

        @Schema(description = "Category ID", example = "123e4567-e89b-12d3-a456-426614174001", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID categoryId,

        @Schema(description = "Product attributes")
        List<CreateProductAttributeRequest> attributes
) {
}