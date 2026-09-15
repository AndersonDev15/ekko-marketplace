package com.ekko.product_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Variant attribute response")
public record VariantAttributeResponse(
        @Schema(description = "Attribute name", example = "Color")
        String name,

        @Schema(description = "Attribute value", example = "Red")
        String value
) {
}