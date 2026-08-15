package com.ekko.review_service.exception;

public class SelfHelpfulVoteException extends RuntimeException {

    public SelfHelpfulVoteException() {
        super("You cannot vote on your own review");
    }
}