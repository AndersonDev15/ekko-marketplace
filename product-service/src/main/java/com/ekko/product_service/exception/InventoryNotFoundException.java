package com.ekko.product_service.exception;

public class InventoryNotFoundException extends RuntimeException {

    public InventoryNotFoundException() {
        super("Inventory not found");
    }

    public InventoryNotFoundException(String message) {
        super(message);
    }
}