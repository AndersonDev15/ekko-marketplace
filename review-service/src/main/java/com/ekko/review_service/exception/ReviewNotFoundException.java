package com.ekko.review_service.exception;

public class ReviewNotFoundException extends RuntimeException {

    public ReviewNotFoundException() {
        super("Review not found");
    }
}