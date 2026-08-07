package com.ekko.product_service.dto.request;

import java.util.UUID;

public record UpdateCategoryRequest(
        String name,
        String slug,
        String description,
        String imageUrl,
        UUID parentId,
        Boolean isActive
) {
}