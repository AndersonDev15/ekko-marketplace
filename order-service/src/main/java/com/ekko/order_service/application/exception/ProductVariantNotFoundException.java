package com.ekko.order_service.application.exception;

import java.util.UUID;

public class ProductVariantNotFoundException extends RuntimeException {

    public ProductVariantNotFoundException(UUID variantId) {
        super("Product variant not found: " + variantId);
    }
}