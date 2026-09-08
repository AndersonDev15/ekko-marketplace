package com.ekko.identity_service.exception;

public class InvalidPasswordException extends RuntimeException {

    private final String detail;

    public InvalidPasswordException(String detail) {
        super("Password does not meet policy requirements: " + detail);
        this.detail = detail;
    }

    public String getDetail() {
        return detail;
    }
}