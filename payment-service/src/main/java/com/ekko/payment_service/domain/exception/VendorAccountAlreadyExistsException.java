package com.ekko.payment_service.domain.exception;

import java.util.UUID;

public class VendorAccountAlreadyExistsException extends RuntimeException {

    private final UUID vendorId;

    public VendorAccountAlreadyExistsException(UUID vendorId) {
        super("Stripe account already exists for vendor " + vendorId);
        this.vendorId = vendorId;
    }

    public UUID getVendorId() {
        return vendorId;
    }
}