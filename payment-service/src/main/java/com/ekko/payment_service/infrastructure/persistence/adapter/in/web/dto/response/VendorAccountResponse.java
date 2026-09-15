package com.ekko.payment_service.infrastructure.persistence.adapter.in.web.dto.response;

import com.ekko.payment_service.domain.model.VendorStripeAccount;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Vendor account response")
public record VendorAccountResponse(
        @Schema(description = "Account UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID id,

        @Schema(description = "Vendor UUID", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
        UUID vendorId,

        @Schema(description = "Stripe Connect account ID", example = "acct_1234567890abcdef")
        String stripeAccountId,

        @Schema(description = "Account status", example = "ACTIVE", allowableValues = {"PENDING", "ACTIVE", "RESTRICTED"})
        String accountStatus,

        @Schema(description = "Whether charges are enabled", example = "true")
        boolean chargesEnabled,

        @Schema(description = "Whether payouts are enabled", example = "false")
        boolean payoutsEnabled,

        @Schema(description = "Account creation timestamp")
        LocalDateTime createdAt,

        @Schema(description = "Account last update timestamp")
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