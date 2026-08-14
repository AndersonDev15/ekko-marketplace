package com.ekko.payment_service.domain.exception;

import java.util.UUID;

public class VendorAccountAlreadyActiveException extends RuntimeException {

    private final UUID vendorId;

    public VendorAccountAlreadyActiveException(UUID vendorId) {
        super("Stripe account is already active for vendor " + vendorId);
        this.vendorId = vendorId;
    }

    public UUID getVendorId() {
        return vendorId;
    }
}