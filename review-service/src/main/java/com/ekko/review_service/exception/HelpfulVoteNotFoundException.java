package com.ekko.review_service.exception;

public class HelpfulVoteNotFoundException extends RuntimeException {

    public HelpfulVoteNotFoundException() {
        super("Helpful vote not found");
    }
}