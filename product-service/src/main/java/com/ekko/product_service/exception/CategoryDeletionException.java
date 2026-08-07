package com.ekko.product_service.exception;

public class CategoryDeletionException extends RuntimeException {

    public CategoryDeletionException() {
        super("Category cannot be deleted");
    }

    public CategoryDeletionException(String message) {
        super(message);
    }
}