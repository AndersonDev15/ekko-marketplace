package com.ekko.product_service.dto.request;

public record CreateImageRequest(
        String url,
        Boolean isPrimary
) {
}
