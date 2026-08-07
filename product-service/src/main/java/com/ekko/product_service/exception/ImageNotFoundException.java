package com.ekko.product_service.exception;

public class ImageNotFoundException extends RuntimeException {

    public ImageNotFoundException() {
        super("Image not found");
    }

    public ImageNotFoundException(String message) {
        super(message);
    }
}
