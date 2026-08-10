package com.ekko.order_service.domain.exception;

public class InvalidGuestEmailException extends RuntimeException {

    public InvalidGuestEmailException(String message) {
        super(message);
    }
}