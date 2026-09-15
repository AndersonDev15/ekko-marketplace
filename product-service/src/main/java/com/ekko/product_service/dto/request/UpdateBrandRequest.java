package com.ekko.product_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to update an existing brand")
public record UpdateBrandRequest(
        @Schema(description = "Brand name", example = "Nike Updated")
        String name,

        @Schema(description = "Brand description", example = "Updated sports apparel and footwear")
        String description
) {
}