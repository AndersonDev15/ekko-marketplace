package com.ekko.product_service.controller;

import com.ekko.product_service.dto.request.CreateVariantRequest;
import com.ekko.product_service.dto.request.UpdateVariantRequest;
import com.ekko.product_service.dto.response.VariantResponse;
import com.ekko.product_service.dto.response.VariantSummaryResponse;
import com.ekko.product_service.service.VariantService;
import com.ekko.product_service.util.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/seller/products/{id}/variants")
@PreAuthorize("hasRole('SELLER')")
@RequiredArgsConstructor
public class VariantController {

    private final VariantService variantService;

    @PostMapping
    public ResponseEntity<VariantResponse> createVariant(
            @PathVariable UUID id,
            @Valid @RequestBody CreateVariantRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        VariantResponse response = variantService.createVariant(id, request, JwtUtils.keycloakId(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{variantId}")
    public ResponseEntity<VariantSummaryResponse> updateVariant(
            @PathVariable UUID id,
            @PathVariable UUID variantId,
            @Valid @RequestBody UpdateVariantRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        VariantSummaryResponse response =
                variantService.updateVariant(id, variantId, request, JwtUtils.keycloakId(jwt));
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{variantId}/desactivate")
    public ResponseEntity<VariantSummaryResponse> desactivateVariant(
            @PathVariable UUID id,
            @PathVariable UUID variantId,
            @AuthenticationPrincipal Jwt jwt) {
        VariantSummaryResponse response =
                variantService.deactivateVariant(id, variantId, JwtUtils.keycloakId(jwt));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{variantId}")
    public ResponseEntity<Void> softDeleteVariant(
            @PathVariable UUID id,
            @PathVariable UUID variantId,
            @AuthenticationPrincipal Jwt jwt) {
        variantService.softDeleteVariant(id, variantId, JwtUtils.keycloakId(jwt));
        return ResponseEntity.noContent().build();
    }
}
