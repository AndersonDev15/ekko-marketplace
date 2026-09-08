package com.ekko.identity_service.exception;

public class KeycloakCommunicationException extends RuntimeException {

    public KeycloakCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public KeycloakCommunicationException(String message) {
        super(message);
    }
}