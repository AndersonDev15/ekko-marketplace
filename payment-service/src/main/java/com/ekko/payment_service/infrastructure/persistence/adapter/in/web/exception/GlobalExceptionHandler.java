package com.ekko.payment_service.infrastructure.persistence.adapter.in.web.exception;

import com.ekko.payment_service.application.exception.PaymentAccessDeniedException;
import com.ekko.payment_service.application.exception.PaymentNotFoundException;
import com.ekko.payment_service.domain.exception.RefundAmountExceededException;
import com.ekko.payment_service.domain.exception.RefundNotAllowedException;
import com.ekko.payment_service.domain.exception.VendorAccountAlreadyActiveException;
import com.ekko.payment_service.domain.exception.VendorAccountAlreadyExistsException;
import com.ekko.payment_service.application.exception.VendorAccountNotFoundException;
import com.ekko.payment_service.infrastructure.gateway.PaymentGatewayException;
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

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFound(PaymentNotFoundException ex) {
        return response(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", ex.getMessage(), ex.getPaymentId());
    }

    @ExceptionHandler(PaymentAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentAccessDenied(PaymentAccessDeniedException ex) {
        return response(HttpStatus.FORBIDDEN, "PAYMENT_ACCESS_DENIED", ex.getMessage(), null);
    }

    @ExceptionHandler(VendorAccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleVendorAccountNotFound(VendorAccountNotFoundException ex) {
        return response(HttpStatus.NOT_FOUND, "VENDOR_ACCOUNT_NOT_FOUND", ex.getMessage(), ex.getVendorId());
    }

    @ExceptionHandler(VendorAccountAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleVendorAccountAlreadyExists(VendorAccountAlreadyExistsException ex) {
        return response(HttpStatus.CONFLICT, "VENDOR_ACCOUNT_ALREADY_EXISTS", ex.getMessage(), ex.getVendorId());
    }

    @ExceptionHandler(VendorAccountAlreadyActiveException.class)
    public ResponseEntity<ErrorResponse> handleVendorAccountAlreadyActive(VendorAccountAlreadyActiveException ex) {
        return response(HttpStatus.CONFLICT, "VENDOR_ACCOUNT_ALREADY_ACTIVE", ex.getMessage(), ex.getVendorId());
    }

    @ExceptionHandler(PaymentGatewayException.class)
    public ResponseEntity<ErrorResponse> handlePaymentGateway(PaymentGatewayException ex) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_GATEWAY_ERROR", ex.getMessage(), null);
    }

    @ExceptionHandler(RefundNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleRefundNotAllowed(RefundNotAllowedException ex) {
        return response(HttpStatus.CONFLICT, "REFUND_NOT_ALLOWED", ex.getMessage(), null);
    }

    @ExceptionHandler(RefundAmountExceededException.class)
    public ResponseEntity<ErrorResponse> handleRefundAmountExceeded(RefundAmountExceededException ex) {
        return response(HttpStatus.CONFLICT, "REFUND_AMOUNT_EXCEEDED", ex.getMessage(), null);
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

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String error, String message, UUID vendorId) {
        ErrorResponse body = new ErrorResponse(error, message, vendorId, status.value());
        return ResponseEntity.status(status).body(body);
    }
}