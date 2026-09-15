package com.ekko.payment_service.infrastructure.persistence.adapter.in.web.controller;

import com.ekko.payment_service.domain.port.in.RefreshOnboardingLinkByStripeAccountUseCase;
import com.ekko.payment_service.infrastructure.persistence.adapter.in.web.dto.response.OnboardingLinkResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vendor-accounts/onboarding")
@RequiredArgsConstructor
@Tag(name = "Stripe Onboarding", description = "Stripe Connect onboarding endpoints for vendor accounts")
public class StripeOnboardingController {

    private final RefreshOnboardingLinkByStripeAccountUseCase
            refreshOnboardingLinkByStripeAccountUseCase;

    @Operation(
            summary = "Refresh onboarding link by Stripe account ID",
            description = "Generates a new Stripe Connect onboarding link for a specific Stripe account ID. Public endpoint - no authentication required."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Onboarding link generated successfully",
                    content = @Content(schema = @Schema(implementation = OnboardingLinkResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid account ID"),
            @ApiResponse(responseCode = "503", description = "Payment gateway unavailable")
    })
    @GetMapping("/refresh")
    public ResponseEntity<OnboardingLinkResponse> refresh(
            @Parameter(description = "Stripe account ID", required = true, example = "acct_1234567890") @RequestParam String accountId) {

        String onboardingUrl =
                refreshOnboardingLinkByStripeAccountUseCase
                        .execute(accountId);

        return ResponseEntity.ok(
                new OnboardingLinkResponse(onboardingUrl)
        );
    }

    @Operation(
            summary = "Stripe onboarding return",
            description = "Callback endpoint for Stripe Connect onboarding completion. Returns no content."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Onboarding completed, no content")
    })
    @GetMapping("/return")
    public ResponseEntity<Void> onboardingReturn() {
        return ResponseEntity.noContent().build();
    }
}
