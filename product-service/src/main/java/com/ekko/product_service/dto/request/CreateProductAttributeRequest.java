package com.ekko.product_service.dto.request;

public record CreateProductAttributeRequest(
        String name,
        String value
) {
}