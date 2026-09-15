package com.ekko.product_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Variant attribute request")
public record CreateVariantAttributeRequest(
        @Schema(description = "Attribute name", example = "Color", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "Attribute value", example = "Red", requiredMode = Schema.RequiredMode.REQUIRED)
        String value
) {
}