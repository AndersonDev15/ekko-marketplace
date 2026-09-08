package com.ekko.identity_service.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return response(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", ex.getMessage(), null);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPassword(InvalidPasswordException ex) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD", ex.getDetail(), null);
    }

    @ExceptionHandler(KeycloakCommunicationException.class)
    public ResponseEntity<ErrorResponse> handleKeycloakCommunication(KeycloakCommunicationException ex) {
        log.error("Keycloak communication failure", ex);
        return response(HttpStatus.SERVICE_UNAVAILABLE, "KEYCLOAK_UNAVAILABLE", "Identity service temporarily unavailable", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                message.isEmpty() ? "Validation failed" : message, null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath().toString() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage(), null);
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String error, String message, UUID keycloakId) {
        ErrorResponse body = new ErrorResponse(error, message, keycloakId, status.value());
        return ResponseEntity.status(status).body(body);
    }
}