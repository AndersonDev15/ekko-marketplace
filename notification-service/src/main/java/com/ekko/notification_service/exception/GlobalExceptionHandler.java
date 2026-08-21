package com.ekko.notification_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotificationNotFound(NotificationNotFoundException ex) {
        return response(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTemplateNotFound(TemplateNotFoundException ex) {
        return response(HttpStatus.NOT_FOUND, "TEMPLATE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(NotificationAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleNotificationAccessDenied(NotificationAccessDeniedException ex) {
        return response(HttpStatus.FORBIDDEN, "NOTIFICATION_ACCESS_DENIED", ex.getMessage());
    }

    @ExceptionHandler(DuplicateTemplateException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateTemplate(DuplicateTemplateException ex) {
        return response(HttpStatus.CONFLICT, "DUPLICATE_TEMPLATE", ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String error, String message) {
        ErrorResponse body = new ErrorResponse(error, message, status.value());
        return ResponseEntity.status(status).body(body);
    }
}