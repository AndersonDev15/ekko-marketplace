package com.ekko.product_service.dto.request;

import java.util.UUID;

public record UpdateProductRequest(
        String name,
        String description,
        UUID brandId,
        UUID categoryId
) {
}