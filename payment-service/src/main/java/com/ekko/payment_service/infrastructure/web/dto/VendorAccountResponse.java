package com.ekko.payment_service.infrastructure.web.dto;

import com.ekko.payment_service.domain.model.VendorStripeAccount;

import java.time.LocalDateTime;
import java.util.UUID;

public record VendorAccountResponse(
        UUID id,
        UUID vendorId,
        String stripeAccountId,
        String accountStatus,
        boolean chargesEnabled,
        boolean payoutsEnabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static VendorAccountResponse from(VendorStripeAccount account) {
        return new VendorAccountResponse(
                account.getId(),
                account.getVendorId(),
                account.getStripeAccountId(),
                account.getAccountStatus().name(),
                account.isChargesEnabled(),
                account.isPayoutsEnabled(),
                account.getCreatedAt(),
                account.getUpdatedAt());
    }
}