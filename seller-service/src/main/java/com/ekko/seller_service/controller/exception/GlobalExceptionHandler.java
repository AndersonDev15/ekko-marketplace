package com.ekko.seller_service.controller.exception;

import com.ekko.seller_service.dto.response.ErrorResponse;
import com.ekko.seller_service.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String UNPROCESSABLE_CONTENT_REASON = "Unprocessable Content";
    private static final String NOT_FOUND_REASON = "Not Found";

    // -------------------------------------------------------------------------
    // 404 NOT FOUND
    // -------------------------------------------------------------------------

    @ExceptionHandler(SellerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSellerNotFound(
            SellerNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, NOT_FOUND_REASON, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(SellerAddressNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAddressNotFound(
            SellerAddressNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, NOT_FOUND_REASON, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(SellerBankAccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBankAccountNotFound(
            SellerBankAccountNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, NOT_FOUND_REASON, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(SellerMetricsNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMetricsNotFound(
            SellerMetricsNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, NOT_FOUND_REASON, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(SellerDocumentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDocumentNotFound(
            SellerDocumentNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, NOT_FOUND_REASON, ex.getMessage(), request.getRequestURI()));
    }

    // -------------------------------------------------------------------------
    // 502 BAD GATEWAY — fallo al subir/borrar documento en MinIO
    // -------------------------------------------------------------------------

    @ExceptionHandler(MinioUploadException.class)
    public ResponseEntity<ErrorResponse> handleMinioUpload(
            MinioUploadException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.of(502, "Bad Gateway", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ImageUploadException.class)
    public ResponseEntity<ErrorResponse> handleImageUpload(
            ImageUploadException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.of(502, "Bad Gateway", ex.getMessage(), request.getRequestURI()));
    }

    // -------------------------------------------------------------------------
    // 409 CONFLICT
    // -------------------------------------------------------------------------

    @ExceptionHandler({
            DuplicateSellerBankAccountException.class,
            InvalidStatusTransitionException.class,
            LastAddressDeleteException.class,
            PrimaryAddressDeleteException.class,
            LastBankAccountDeleteException.class,
            PrimaryBankAccountDeleteException.class,
            DocumentAlreadyPendingException.class,
            DocumentAlreadyApprovedException.class,
            DocumentAlreadyReviewedException.class
    })
    public ResponseEntity<ErrorResponse> handleConflict(
            RuntimeException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage(), request.getRequestURI()));
    }

    // -------------------------------------------------------------------------
    // 422 UNPROCESSABLE ENTITY — estado de cuenta inválido para la operación
    // -------------------------------------------------------------------------

    @ExceptionHandler(SellerSuspendedException.class)
    public ResponseEntity<ErrorResponse> handleSellerSuspended(
            SellerSuspendedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(ErrorResponse.of(422, UNPROCESSABLE_CONTENT_REASON, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(SellerPendingException.class)
    public ResponseEntity<ErrorResponse> handleSellerPending(
            SellerPendingException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(ErrorResponse.of(422, UNPROCESSABLE_CONTENT_REASON, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(SellerAlreadyActiveException.class)
    public ResponseEntity<ErrorResponse> handleSellerAlreadyActive(
            SellerAlreadyActiveException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(ErrorResponse.of(422, UNPROCESSABLE_CONTENT_REASON, ex.getMessage(), request.getRequestURI()));
    }


    // -------------------------------------------------------------------------
    // 400 BAD REQUEST — validación de @Valid
    // -------------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", "Validation failed", request.getRequestURI(), fieldErrors));
    }

    @ExceptionHandler({
            MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleMalformedRequest(
            Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", "Malformed request", request.getRequestURI()));
    }

    // -------------------------------------------------------------------------
    // 403 FORBIDDEN
    // -------------------------------------------------------------------------

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden", "Access denied", request.getRequestURI()));
    }

    // -------------------------------------------------------------------------
    // 500 INTERNAL SERVER ERROR — catch-all
    // -------------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Internal Server Error", "An unexpected error occurred", request.getRequestURI()));
    }
}
