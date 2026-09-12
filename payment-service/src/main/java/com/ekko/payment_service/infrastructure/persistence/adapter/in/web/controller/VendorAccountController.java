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
public class VendorAccountController {

    private final CreateVendorAccountUseCase createVendorAccountUseCase;
    private final RefreshOnboardingLinkUseCase refreshOnboardingLinkUseCase;
    private final GetVendorAccountUseCase getVendorAccountUseCase;

    @PostMapping
    public ResponseEntity<CreateVendorAccountResponse> createVendorAccount(
            @Valid @RequestBody CreateVendorAccountRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID vendorId = keycloakId(jwt);
        VendorAccountResult result = createVendorAccountUseCase.execute(
                new CreateVendorAccountCommand(vendorId, request.country(), request.email()));

        CreateVendorAccountResponse response = new CreateVendorAccountResponse(
                VendorAccountResponse.from(result.vendorStripeAccount()),
                result.onboardingUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/refresh-link")
    public ResponseEntity<OnboardingLinkResponse> refreshOnboardingLink(
            @AuthenticationPrincipal Jwt jwt) {

        String onboardingUrl = refreshOnboardingLinkUseCase.execute(keycloakId(jwt));
        return ResponseEntity.ok(new OnboardingLinkResponse(onboardingUrl));
    }

    @GetMapping("/me")
    public ResponseEntity<VendorAccountResponse> getMyVendorAccount(
            @AuthenticationPrincipal Jwt jwt) {

        VendorStripeAccount account = getVendorAccountUseCase.execute(keycloakId(jwt));
        return ResponseEntity.ok(VendorAccountResponse.from(account));
    }

    private UUID keycloakId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}