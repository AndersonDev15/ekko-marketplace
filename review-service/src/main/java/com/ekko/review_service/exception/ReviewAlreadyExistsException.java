package com.ekko.review_service.exception;

public class ReviewAlreadyExistsException extends RuntimeException {

    public ReviewAlreadyExistsException() {
        super("A review for this item already exists");
    }
}