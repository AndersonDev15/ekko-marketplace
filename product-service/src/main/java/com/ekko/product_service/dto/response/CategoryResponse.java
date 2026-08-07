package com.ekko.product_service.dto.response;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        Boolean isActive
) {
}