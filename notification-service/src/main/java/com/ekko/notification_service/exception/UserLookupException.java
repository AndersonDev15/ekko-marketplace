package com.ekko.notification_service.exception;

public class UserLookupException extends RuntimeException {

    public UserLookupException(String message) {
        super(message);
    }

    public UserLookupException(String message, Throwable cause) {
        super(message, cause);
    }
}