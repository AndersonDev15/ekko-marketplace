package com.ekko.review_service.web;

import com.ekko.review_service.exception.HelpfulVoteAlreadyExistsException;
import com.ekko.review_service.exception.HelpfulVoteNotFoundException;
import com.ekko.review_service.exception.NotEligibleToReviewException;
import com.ekko.review_service.exception.ReviewAlreadyExistsException;
import com.ekko.review_service.exception.ReviewEditWindowExpiredException;
import com.ekko.review_service.exception.ReviewNotFoundException;
import com.ekko.review_service.exception.ReviewOwnershipException;
import com.ekko.review_service.exception.SelfHelpfulVoteException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotEligibleToReviewException.class)
    public ResponseEntity<ErrorResponse> handleNotEligibleToReview(NotEligibleToReviewException ex) {
        return response(HttpStatus.FORBIDDEN, "NOT_ELIGIBLE_TO_REVIEW", ex.getMessage());
    }

    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReviewNotFound(ReviewNotFoundException ex) {
        return response(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(ReviewAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleReviewAlreadyExists(ReviewAlreadyExistsException ex) {
        return response(HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS", ex.getMessage());
    }

    @ExceptionHandler(ReviewOwnershipException.class)
    public ResponseEntity<ErrorResponse> handleReviewOwnership(ReviewOwnershipException ex) {
        return response(HttpStatus.FORBIDDEN, "REVIEW_OWNERSHIP_VIOLATION", ex.getMessage());
    }

    @ExceptionHandler(ReviewEditWindowExpiredException.class)
    public ResponseEntity<ErrorResponse> handleReviewEditWindowExpired(ReviewEditWindowExpiredException ex) {
        return response(HttpStatus.FORBIDDEN, "REVIEW_EDIT_WINDOW_EXPIRED", ex.getMessage());
    }

    @ExceptionHandler(SelfHelpfulVoteException.class)
    public ResponseEntity<ErrorResponse> handleSelfHelpfulVote(SelfHelpfulVoteException ex) {
        return response(HttpStatus.FORBIDDEN, "SELF_HELPFUL_VOTE_NOT_ALLOWED", ex.getMessage());
    }

    @ExceptionHandler(HelpfulVoteAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleHelpfulVoteAlreadyExists(HelpfulVoteAlreadyExistsException ex) {
        return response(HttpStatus.CONFLICT, "HELPFUL_VOTE_ALREADY_EXISTS", ex.getMessage());
    }

    @ExceptionHandler(HelpfulVoteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleHelpfulVoteNotFound(HelpfulVoteNotFoundException ex) {
        return response(HttpStatus.NOT_FOUND, "HELPFUL_VOTE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                message.isEmpty() ? "Validation failed" : message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath().toString() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String error, String message) {
        ErrorResponse body = new ErrorResponse(error, message, status.value());
        return ResponseEntity.status(status).body(body);
    }
}