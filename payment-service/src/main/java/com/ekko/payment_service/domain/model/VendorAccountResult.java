package com.ekko.payment_service.domain.model;

public record VendorAccountResult(
        VendorStripeAccount vendorStripeAccount,
        String onboardingUrl
) {
}