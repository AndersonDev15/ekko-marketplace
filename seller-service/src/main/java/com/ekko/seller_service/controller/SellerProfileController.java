package com.ekko.seller_service.controller;

import com.ekko.seller_service.dto.response.ErrorResponse;
import com.ekko.seller_service.dto.response.SellerLogoResponse;
import com.ekko.seller_service.dto.response.SellerResponse;
import com.ekko.seller_service.dto.request.SellerUpdateRequest;
import com.ekko.seller_service.support.SellerResolver;
import com.ekko.seller_service.service.SellerProfileService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Seller Profile", description = "Endpoints for managing seller profile")
public class SellerProfileController {

    private final SellerResolver sellerResolver;
    private final SellerProfileService sellerProfileService;

    // ── Rutas autenticadas (/sellers/me) ──────────────────────────────────

    @Operation(
            summary = "Get my seller profile",
            description = "Returns the authenticated seller's profile. Creates a new profile if it doesn't exist.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SellerResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/sellers/me")
    @PreAuthorize("hasRole('SELLER')")
    public SellerResponse getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return sellerProfileService.getOrCreateMyProfile(
                jwt.getSubject(),
                jwt.getClaimAsString("email")
        );
    }

    @Operation(
            summary = "Update my seller profile",
            description = "Updates the authenticated seller's profile information",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = SellerResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Unprocessable - seller state invalid for operation",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/sellers/me")
    @PreAuthorize("hasRole('SELLER')")
    public SellerResponse updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid SellerUpdateRequest request
    ) {
        return sellerProfileService.updateMyProfile(jwt.getSubject(), request);
    }

    @Operation(
            summary = "Upload seller logo",
            description = "Uploads a logo image for the authenticated seller's store",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logo uploaded successfully",
                    content = @Content(schema = @Schema(implementation = SellerLogoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file or missing file parameter",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "Bad Gateway - failed to upload to storage",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/sellers/me/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SellerLogoResponse> uploadLogo(
            @Parameter(description = "Logo image file", required = true)
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {

        String keycloakId = jwt.getSubject();
        SellerLogoResponse response = sellerProfileService.uploadLogo(keycloakId, file);
        return ResponseEntity.ok(response);
    }

    // ── Rutas públicas (/sellers/{id}) ────────────────────────────────────

    @Operation(
            summary = "Get public seller profile",
            description = "Returns public information about a seller by ID. No authentication required.",
            security = { }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Public profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SellerResponse.class))),
            @ApiResponse(responseCode = "404", description = "Seller not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/sellers/{id}")
    public SellerResponse getPublicProfile(
            @Parameter(description = "Seller UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        return sellerProfileService.getPublicProfile(id);
    }
}