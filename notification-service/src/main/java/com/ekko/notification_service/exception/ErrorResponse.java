package com.ekko.notification_service.exception;

public record ErrorResponse(
        String error,
        String message,
        Integer httpStatus
) {
}