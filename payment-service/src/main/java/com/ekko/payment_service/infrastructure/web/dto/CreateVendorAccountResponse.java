package com.ekko.payment_service.infrastructure.web.dto;

public record CreateVendorAccountResponse(
        VendorAccountResponse account,
        String onboardingUrl
) {
}