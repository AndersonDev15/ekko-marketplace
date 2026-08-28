package com.ekko.product_service.dto.request;

public record CreateBrandRequest(
        String name,
        String description
) {
}