package com.ekko.product_service.dto.response;

import java.util.UUID;

public record BrandResponse(
        UUID id,
        String name,
        String slug,
        String logoUrl,
        String description,
        Boolean isActive
) {
}