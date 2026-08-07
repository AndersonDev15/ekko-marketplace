package com.ekko.product_service.dto.response;

import java.util.List;
import java.util.UUID;

public record CategoryNodeResponse(
        UUID id,
        String name,
        String slug,
        List<CategoryNodeResponse> children
) {
}