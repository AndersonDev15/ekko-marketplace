package com.ekko.seller_service.controller;

import com.ekko.seller_service.dto.response.SellerResponse;
import com.ekko.seller_service.dto.request.SellerUpdateRequest;
import com.ekko.seller_service.support.SellerResolver;
import com.ekko.seller_service.service.SellerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SellerProfileController {

    private final SellerResolver sellerResolver;
    private final SellerProfileService sellerProfileService;

    // ── Rutas autenticadas (/sellers/me) ──────────────────────────────────

    @GetMapping("/sellers/me")
    @PreAuthorize("hasRole('SELLER')")
    public SellerResponse getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return sellerProfileService.getOrCreateMyProfile(
                jwt.getSubject(),
                jwt.getClaimAsString("email")
        );
    }

    @PutMapping("/sellers/me")
    @PreAuthorize("hasRole('SELLER')")
    public SellerResponse updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid SellerUpdateRequest request
    ) {
        return sellerProfileService.updateMyProfile(jwt.getSubject(), request);
    }

    // ── Rutas públicas (/sellers/{id}) ────────────────────────────────────

    @GetMapping("/sellers/{id}")
    public SellerResponse getPublicProfile(@PathVariable UUID id) {
        return sellerProfileService.getPublicProfile(id);
    }
}