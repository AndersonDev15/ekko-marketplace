package com.ekko.payment_service.infrastructure.persistence.adapter.in.web.controller;

import com.ekko.payment_service.domain.port.in.RefreshOnboardingLinkByStripeAccountUseCase;
import com.ekko.payment_service.infrastructure.persistence.adapter.in.web.dto.response.OnboardingLinkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vendor-accounts/onboarding")
@RequiredArgsConstructor
public class StripeOnboardingController {

    private final RefreshOnboardingLinkByStripeAccountUseCase
            refreshOnboardingLinkByStripeAccountUseCase;

    @GetMapping("/refresh")
    public ResponseEntity<OnboardingLinkResponse> refresh(
            @RequestParam String accountId) {

        String onboardingUrl =
                refreshOnboardingLinkByStripeAccountUseCase
                        .execute(accountId);

        return ResponseEntity.ok(
                new OnboardingLinkResponse(onboardingUrl)
        );
    }

    @GetMapping("/return")
    public ResponseEntity<Void> onboardingReturn() {
        return ResponseEntity.noContent().build();
    }
}
