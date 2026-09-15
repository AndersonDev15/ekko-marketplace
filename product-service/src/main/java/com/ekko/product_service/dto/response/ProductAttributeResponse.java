package com.ekko.product_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Product attribute response")
public record ProductAttributeResponse(
        @Schema(description = "Attribute ID", example = "123e4567-e89b-12d3-a456-426614174009")
        UUID id,

        @Schema(description = "Attribute name", example = "Material")
        String name,

        @Schema(description = "Attribute value", example = "Cotton")
        String value
) {
}