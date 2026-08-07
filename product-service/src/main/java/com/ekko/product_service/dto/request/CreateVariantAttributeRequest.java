package com.ekko.product_service.dto.request;

public record CreateVariantAttributeRequest(
        String name,
        String value
) {
}