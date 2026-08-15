package com.ekko.review_service.exception;

public class NotEligibleToReviewException extends RuntimeException {

    public NotEligibleToReviewException() {
        super("You are not eligible to review this item");
    }
}