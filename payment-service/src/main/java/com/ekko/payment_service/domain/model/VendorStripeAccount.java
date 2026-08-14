package com.ekko.payment_service.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class VendorStripeAccount {

    private final UUID id;
    private final UUID vendorId;
    private final String stripeAccountId;
    private VendorAccountStatus accountStatus;
    private boolean chargesEnabled;
    private boolean payoutsEnabled;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private VendorStripeAccount(UUID id, UUID vendorId, String stripeAccountId,
                                VendorAccountStatus accountStatus, boolean chargesEnabled,
                                boolean payoutsEnabled, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.vendorId = vendorId;
        this.stripeAccountId = stripeAccountId;
        this.accountStatus = accountStatus;
        this.chargesEnabled = chargesEnabled;
        this.payoutsEnabled = payoutsEnabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static VendorStripeAccount initiate(UUID vendorId, String stripeAccountId) {
        LocalDateTime now = LocalDateTime.now();
        return new VendorStripeAccount(
                null,
                vendorId,
                stripeAccountId,
                VendorAccountStatus.PENDING,
                false,
                false,
                now,
                now);
    }

    public static VendorStripeAccount restore(UUID id, UUID vendorId, String stripeAccountId,
                                              VendorAccountStatus accountStatus, boolean chargesEnabled,
                                              boolean payoutsEnabled, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new VendorStripeAccount(
                id,
                vendorId,
                stripeAccountId,
                accountStatus,
                chargesEnabled,
                payoutsEnabled,
                createdAt,
                updatedAt);
    }

    public void applyAccountUpdate(boolean chargesEnabled, boolean payoutsEnabled,
                                   boolean disabledReasonPresent, boolean hasPendingRequirements) {
        if (disabledReasonPresent) {
            this.accountStatus = VendorAccountStatus.DISABLED;
        } else if (chargesEnabled && payoutsEnabled) {
            this.accountStatus = VendorAccountStatus.ACTIVE;
        } else if (hasPendingRequirements) {
            this.accountStatus = VendorAccountStatus.RESTRICTED;
        } else {
            this.accountStatus = VendorAccountStatus.PENDING;
        }
        this.chargesEnabled = chargesEnabled;
        this.payoutsEnabled = payoutsEnabled;
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getVendorId() {
        return vendorId;
    }

    public String getStripeAccountId() {
        return stripeAccountId;
    }

    public VendorAccountStatus getAccountStatus() {
        return accountStatus;
    }

    public boolean isChargesEnabled() {
        return chargesEnabled;
    }

    public boolean isPayoutsEnabled() {
        return payoutsEnabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}