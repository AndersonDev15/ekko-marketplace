package com.ekko.product_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Product attribute request")
public record CreateProductAttributeRequest(
        @Schema(description = "Attribute name", example = "Material", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "Attribute value", example = "Cotton", requiredMode = Schema.RequiredMode.REQUIRED)
        String value
) {
}