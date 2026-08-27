package com.ekko.product_service.dto.request;



public record UpdateCategoryRequest(
        String name,
        String slug,
        String description
) {
}