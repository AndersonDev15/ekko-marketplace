package com.ekko.product_service.dto.request;

public record UpdateBrandRequest(
        String name,
        String slug,
        String description
) {
}