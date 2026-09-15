package com.ekko.payment_service.infrastructure.persistence.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Onboarding link response")
public record OnboardingLinkResponse(
        @Schema(description = "Stripe Connect onboarding URL", example = "https://connect.stripe.com/setup/s/1234567890")
        String onboardingUrl
) {
}