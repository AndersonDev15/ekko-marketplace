package com.ekko.review_service.exception;

public class ReviewEditWindowExpiredException extends RuntimeException {

    public ReviewEditWindowExpiredException() {
        super("The 15-day review editing window has expired");
    }
}