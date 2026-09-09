package com.ekko.identity_service.exception;

public class StepUpAuthRequiredException extends RuntimeException {

    public StepUpAuthRequiredException(String message) {
        super(message);
    }
}