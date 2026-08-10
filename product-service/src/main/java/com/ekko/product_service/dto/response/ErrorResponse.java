package com.ekko.product_service.dto.response;

import java.util.UUID;

public record ErrorResponse(
        String error,
        String message,
        UUID variantId,
        Integer httpStatus
) {
}