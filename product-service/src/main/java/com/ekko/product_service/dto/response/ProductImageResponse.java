package com.ekko.product_service.dto.response;

import java.util.UUID;

public record ProductImageResponse(
        UUID id,
        String url,
        Boolean isPrimary,
        Integer sortOrder
) {
}