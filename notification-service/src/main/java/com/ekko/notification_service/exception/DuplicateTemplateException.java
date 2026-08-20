package com.ekko.notification_service.exception;

public class DuplicateTemplateException extends RuntimeException {

    public DuplicateTemplateException(String message) {
        super(message);
    }
}