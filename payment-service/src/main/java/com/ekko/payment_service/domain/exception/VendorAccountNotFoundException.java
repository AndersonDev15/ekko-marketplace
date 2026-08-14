package com.ekko.payment_service.domain.exception;

import java.util.UUID;

public class VendorAccountNotFoundException extends RuntimeException {

    private final UUID vendorId;

    public VendorAccountNotFoundException(UUID vendorId) {
        super("Stripe account not found for vendor " + vendorId);
        this.vendorId = vendorId;
    }

    public VendorAccountNotFoundException(String stripeAccountId) {
        super("Stripe account not found for stripe account id " + stripeAccountId);
        this.vendorId = null;
    }

    public UUID getVendorId() {
        return vendorId;
    }
}