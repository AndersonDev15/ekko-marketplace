package com.ekko.product_service.dto.request;

import java.util.UUID;

public record CreateCategoryRequest(
        String name,
        String description,
        UUID parentId
) {
}