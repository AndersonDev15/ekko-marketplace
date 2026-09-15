package com.ekko.product_service.controller;

import com.ekko.product_service.dto.request.CreateVariantRequest;
import com.ekko.product_service.dto.request.UpdateVariantRequest;
import com.ekko.product_service.dto.response.VariantResponse;
import com.ekko.product_service.dto.response.VariantSummaryResponse;
import com.ekko.product_service.service.VariantService;
import com.ekko.product_service.util.JwtUtils;
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
@Tag(name = "Seller Variants", description = "Product variant management endpoints for sellers")
@SecurityRequirement(name = "bearerAuth")
public class VariantController {

    private final VariantService variantService;

    @Operation(summary = "Create variant", description = "Creates a new variant for a product owned by the authenticated seller")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Variant created successfully",
                    content = @Content(schema = @Schema(implementation = VariantResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role"),
            @ApiResponse(responseCode = "404", description = "Product not found or access denied"),
            @ApiResponse(responseCode = "409", description = "Conflict - duplicate SKU")
    })
    @PostMapping
    public ResponseEntity<VariantResponse> createVariant(
            @Parameter(description = "Product ID") @PathVariable UUID id,
            @Parameter(description = "Variant creation request") @Valid @RequestBody CreateVariantRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        VariantResponse response = variantService.createVariant(id, request, JwtUtils.keycloakId(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update variant", description = "Updates an existing variant for a product owned by the authenticated seller")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Variant updated successfully",
                    content = @Content(schema = @Schema(implementation = VariantSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role"),
            @ApiResponse(responseCode = "404", description = "Product or variant not found or access denied"),
            @ApiResponse(responseCode = "409", description = "Conflict - duplicate SKU")
    })
    @PutMapping("/{variantId}")
    public ResponseEntity<VariantSummaryResponse> updateVariant(
            @Parameter(description = "Product ID") @PathVariable UUID id,
            @Parameter(description = "Variant ID") @PathVariable UUID variantId,
            @Parameter(description = "Variant update request") @Valid @RequestBody UpdateVariantRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        VariantSummaryResponse response =
                variantService.updateVariant(id, variantId, request, JwtUtils.keycloakId(jwt));
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Deactivate variant", description = "Deactivates a variant for a product owned by the authenticated seller")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Variant deactivated successfully",
                    content = @Content(schema = @Schema(implementation = VariantSummaryResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role"),
            @ApiResponse(responseCode = "404", description = "Product or variant not found or access denied"),
            @ApiResponse(responseCode = "409", description = "Conflict - cannot deactivate last active variant")
    })
    @PatchMapping("/{variantId}/desactivate")
    public ResponseEntity<VariantSummaryResponse> desactivateVariant(
            @Parameter(description = "Product ID") @PathVariable UUID id,
            @Parameter(description = "Variant ID") @PathVariable UUID variantId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        VariantSummaryResponse response =
                variantService.deactivateVariant(id, variantId, JwtUtils.keycloakId(jwt));
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Soft delete variant", description = "Soft deletes a variant for a product owned by the authenticated seller")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Variant deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role"),
            @ApiResponse(responseCode = "404", description = "Product or variant not found or access denied"),
            @ApiResponse(responseCode = "409", description = "Conflict - cannot delete last active variant")
    })
    @DeleteMapping("/{variantId}")
    public ResponseEntity<Void> softDeleteVariant(
            @Parameter(description = "Product ID") @PathVariable UUID id,
            @Parameter(description = "Variant ID") @PathVariable UUID variantId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        variantService.softDeleteVariant(id, variantId, JwtUtils.keycloakId(jwt));
        return ResponseEntity.noContent().build();
    }
}
