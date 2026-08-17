package com.ekko.payment_service.infrastructure.persistence.adapter.in.web.dto.response;

public record CreateVendorAccountResponse(
        VendorAccountResponse account,
        String onboardingUrl
) {
}