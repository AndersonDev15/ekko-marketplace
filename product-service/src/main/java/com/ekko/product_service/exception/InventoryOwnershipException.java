package com.ekko.product_service.exception;

public class InventoryOwnershipException extends RuntimeException {

    public InventoryOwnershipException() {
        super("Inventory ownership not authorized");
    }

    public InventoryOwnershipException(String message) {
        super(message);
    }
}