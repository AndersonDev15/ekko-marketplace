package com.ekko.review_service.exception;

public class ReviewImageLimitExceededException extends RuntimeException {
    public ReviewImageLimitExceededException() {
        super("Review image limit exceeded");
    }
}