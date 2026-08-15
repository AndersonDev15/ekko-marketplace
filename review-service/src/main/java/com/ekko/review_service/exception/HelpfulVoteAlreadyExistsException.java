package com.ekko.review_service.exception;

public class HelpfulVoteAlreadyExistsException extends RuntimeException {

    public HelpfulVoteAlreadyExistsException() {
        super("You have already voted on this review");
    }
}