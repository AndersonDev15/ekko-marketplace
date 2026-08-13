package com.ekko.payment_service.domain.exception;

import java.util.UUID;

public class VendorAccountNotActiveException extends RuntimeException {

    private final UUID vendorId;

    public VendorAccountNotActiveException(UUID vendorId) {
        super("Stripe account is not active for vendor " + vendorId);
        this.vendorId = vendorId;
    }

    public UUID getVendorId() {
        return vendorId;
    }
}
