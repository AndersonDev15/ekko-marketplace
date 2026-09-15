package com.ekko.payment_service.infrastructure.persistence.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Create vendor account response")
public record CreateVendorAccountResponse(
        @Schema(description = "Vendor account details")
        VendorAccountResponse account,

        @Schema(description = "Stripe Connect onboarding URL", example = "https://connect.stripe.com/setup/s/1234567890")
        String onboardingUrl
) {
}