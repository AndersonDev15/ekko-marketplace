package com.ekko.product_service.dto.request;

import java.util.List;
import java.util.UUID;

public record CreateProductRequest(
        String name,
        String description,
        UUID brandId,
        UUID categoryId,
        List<CreateProductAttributeRequest> attributes
) {
}