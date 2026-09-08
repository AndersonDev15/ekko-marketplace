package com.ekko.identity_service.exception;

import java.util.UUID;

public record ErrorResponse(
        String error,
        String message,
        UUID keycloakId,
        Integer httpStatus
) {
}