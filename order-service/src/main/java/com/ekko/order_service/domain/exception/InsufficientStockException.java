package com.ekko.order_service.domain.exception;

import java.util.UUID;

public class InsufficientStockException extends RuntimeException {

    private final UUID variantId;
    private final long requestedQuantity;

    public InsufficientStockException(UUID variantId, long requestedQuantity) {
        super("Insufficient stock for variant " + variantId + " (requested " + requestedQuantity + ")");
        this.variantId = variantId;
        this.requestedQuantity = requestedQuantity;
    }

    public InsufficientStockException(String message) {
        super(message);
        this.variantId = null;
        this.requestedQuantity = 0;
    }

    public UUID getVariantId() {
        return variantId;
    }

    public long getRequestedQuantity() {
        return requestedQuantity;
    }
}