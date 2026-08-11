package com.ekko.order_service.infrastructure.web;

import java.util.UUID;

public record ErrorResponse(
        String error,
        String message,
        UUID variantId,
        Integer httpStatus
) {
}