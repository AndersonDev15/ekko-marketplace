package com.ekko.order_service.infrastructure.persistence.adapter.in.web.exception;

import com.ekko.order_service.domain.exception.InsufficientStockException;
import com.ekko.order_service.domain.exception.InvalidGuestEmailException;
import com.ekko.order_service.domain.exception.InvalidOrderStatusTransitionException;
import com.ekko.order_service.application.exception.OrderAccessDeniedException;
import com.ekko.order_service.domain.exception.OrderCancellationNotAllowedException;
import com.ekko.order_service.application.exception.OrderNotFoundException;
import com.ekko.order_service.application.exception.OrderNumberGenerationException;
import com.ekko.order_service.application.exception.StockReservationException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        return response(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", ex.getMessage(), null);
    }

    @ExceptionHandler(OrderAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleOrderAccessDenied(OrderAccessDeniedException ex) {
        return response(HttpStatus.FORBIDDEN, "ORDER_ACCESS_DENIED", ex.getMessage(), null);
    }

    @ExceptionHandler(OrderCancellationNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleOrderCancellationNotAllowed(OrderCancellationNotAllowedException ex) {
        return response(HttpStatus.CONFLICT, "CANCELLATION_NOT_ALLOWED", ex.getMessage(), null);
    }

    @ExceptionHandler(InvalidOrderStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrderStatusTransition(InvalidOrderStatusTransitionException ex) {
        return response(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION", ex.getMessage(), null);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException ex) {
        return response(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", ex.getMessage(), ex.getVariantId());
    }

    @ExceptionHandler(StockReservationException.class)
    public ResponseEntity<ErrorResponse> handleStockReservation(StockReservationException ex) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "STOCK_SERVICE_UNAVAILABLE", ex.getMessage(), null);
    }

    @ExceptionHandler(InvalidGuestEmailException.class)
    public ResponseEntity<ErrorResponse> handleInvalidGuestEmail(InvalidGuestEmailException ex) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_GUEST_EMAIL", ex.getMessage(), null);
    }

    @ExceptionHandler(OrderNumberGenerationException.class)
    public ResponseEntity<ErrorResponse> handleOrderNumberGeneration(OrderNumberGenerationException ex) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "ORDER_NUMBER_GENERATION_FAILED", ex.getMessage(), null);
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

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String error, String message, UUID variantId) {
        ErrorResponse body = new ErrorResponse(error, message, variantId, status.value());
        return ResponseEntity.status(status).body(body);
    }
}