package com.ekko.review_service.exception;

public record ErrorResponse(
        String error,
        String message,
        Integer httpStatus
) {
}