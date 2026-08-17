package com.ekko.order_service.infrastructure.persistence.adapter.in.web.exception;

import java.util.UUID;

public record ErrorResponse(
        String error,
        String message,
        UUID variantId,
        Integer httpStatus
) {
}