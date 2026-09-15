package com.ekko.product_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to create a new brand")
public record CreateBrandRequest(
        @Schema(description = "Brand name", example = "Nike", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "Brand description", example = "Sports apparel and footwear")
        String description
) {
}