package com.ekko.review_service.web;

public record ErrorResponse(
        String error,
        String message,
        Integer httpStatus
) {
}