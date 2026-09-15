package com.ekko.seller_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SellerLogoResponse(
        @Schema(description = "Uploaded logo URL", example = "https://cdn.example.com/logos/store123.png")
        String logoUrl
) {}