package com.ekko.review_service.exception;

public class ReviewImageNotFoundException extends RuntimeException {
    public ReviewImageNotFoundException() {
        super("Review image not found");
    }
}
