package com.ekko.product_service.dto.response;

import java.util.UUID;

public record ProductAttributeResponse(
        UUID id,
        String name,
        String value
) {
}