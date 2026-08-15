package com.ekko.review_service.exception;

public class ReviewOwnershipException extends RuntimeException {

    public ReviewOwnershipException() {
        super("Access denied to this review");
    }
}