package com.ekko.seller_service.exception;

public class MinioUploadException extends RuntimeException {

    public MinioUploadException(String message) {
        super(message);
    }

    public MinioUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}