package com.ekko.payment_service.infrastructure.persistence.adapter.in.web.controller;

import com.ekko.payment_service.domain.command.CreateVendorAccountCommand;
import com.ekko.payment_service.domain.model.VendorAccountResult;
import com.ekko.payment_service.domain.model.VendorStripeAccount;
import com.ekko.payment_service.domain.port.in.CreateVendorAccountUseCase;
import com.ekko.payment_service.domain.port.in.GetVendorAccountUseCase;
import com.ekko.payment_service.domain.port.in.RefreshOnboardingLinkUseCase;
import com.ekko.payment_service.infrastructure.persistence.adapter.in.web.dto.request.CreateVendorAccountRequest;
import com.ekko.payment_service.infrastructure.persistence.adapter.in.web.dto.response.CreateVendorAccountResponse;
import com.ekko.payment_service.infrastructure.persistence.adapter.in.web.dto.response.OnboardingLinkResponse;
import com.ekko.payment_service.infrastructure.persistence.adapter.in.web.dto.response.VendorAccountResponse;
import com.ekko.payment_service.infrastructure.persistence.adapter.in.web.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/vendor-accounts")
@PreAuthorize("hasRole('SELLER')")
@RequiredArgsConstructor
@Tag(name = "Vendor Accounts", description = "Seller vendor account management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class VendorAccountController {

    private final CreateVendorAccountUseCase createVendorAccountUseCase;
    private final RefreshOnboardingLinkUseCase refreshOnboardingLinkUseCase;
    private final GetVendorAccountUseCase getVendorAccountUseCase;

    @Operation(
            summary = "Create a new vendor account",
            description = "Creates a new Stripe Connect vendor account for the authenticated seller. Returns the vendor account details and an onboarding URL to complete Stripe setup."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Vendor account created successfully",
                    content = @Content(schema = @Schema(implementation = CreateVendorAccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role"),
            @ApiResponse(responseCode = "409", description = "Vendor account already exists or already active",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Payment gateway unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<CreateVendorAccountResponse> createVendorAccount(
            @Valid @RequestBody CreateVendorAccountRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

        UUID vendorId = keycloakId(jwt);
        VendorAccountResult result = createVendorAccountUseCase.execute(
                new CreateVendorAccountCommand(vendorId, request.country(), request.email()));

        CreateVendorAccountResponse response = new CreateVendorAccountResponse(
                VendorAccountResponse.from(result.vendorStripeAccount()),
                result.onboardingUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Refresh onboarding link",
            description = "Generates a new Stripe Connect onboarding link for the authenticated seller's existing vendor account."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Onboarding link generated successfully",
                    content = @Content(schema = @Schema(implementation = OnboardingLinkResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role"),
            @ApiResponse(responseCode = "404", description = "Vendor account not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Payment gateway unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh-link")
    public ResponseEntity<OnboardingLinkResponse> refreshOnboardingLink(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

        String onboardingUrl = refreshOnboardingLinkUseCase.execute(keycloakId(jwt));
        return ResponseEntity.ok(new OnboardingLinkResponse(onboardingUrl));
    }

    @Operation(
            summary = "Get my vendor account",
            description = "Returns the vendor account details for the authenticated seller."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vendor account retrieved successfully",
                    content = @Content(schema = @Schema(implementation = VendorAccountResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role"),
            @ApiResponse(responseCode = "404", description = "Vendor account not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<VendorAccountResponse> getMyVendorAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

        VendorStripeAccount account = getVendorAccountUseCase.execute(keycloakId(jwt));
        return ResponseEntity.ok(VendorAccountResponse.from(account));
    }

    private UUID keycloakId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}