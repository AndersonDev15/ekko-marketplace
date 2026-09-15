package com.ekko.seller_service.controller;

import com.ekko.seller_service.dto.request.ReviewDocumentRequest;
import com.ekko.seller_service.dto.request.UpdateSellerStatusRequest;
import com.ekko.seller_service.dto.response.ErrorResponse;
import com.ekko.seller_service.dto.response.SellerDetailResponse;
import com.ekko.seller_service.dto.response.SellerDocumentResponse;
import com.ekko.seller_service.dto.response.SellerResponse;
import com.ekko.seller_service.dto.response.SellerSummaryResponse;
import com.ekko.seller_service.enums.SellerStatus;
import com.ekko.seller_service.service.AdminSellerService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/admin/sellers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Sellers", description = "Admin endpoints for managing sellers")
public class AdminSellerController {

    private final AdminSellerService adminSellerService;

    @Operation(
            summary = "Get all sellers (paginated)",
            description = "Returns a paginated list of sellers with optional filters by status and date range",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sellers retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SellerSummaryResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<Page<SellerSummaryResponse>> getAllSellers(
            @Parameter(description = "Filter by seller status", schema = @Schema(implementation = SellerStatus.class))
            @RequestParam(required = false) SellerStatus status,
            @Parameter(description = "Filter by creation date from (ISO 8601)", example = "2024-01-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Filter by creation date to (ISO 8601)", example = "2024-12-31T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable) {
        return ResponseEntity.ok(adminSellerService.getAllSellers(status, from, to, pageable));
    }

    @Operation(
            summary = "Get seller detail",
            description = "Returns detailed information about a specific seller including documents and metrics",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Seller detail retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SellerDetailResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Seller not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<SellerDetailResponse> getSellerDetail(
            @Parameter(description = "Seller UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        return ResponseEntity.ok(adminSellerService.getSellerDetail(id));
    }

    @Operation(
            summary = "Update seller status",
            description = "Updates the status of a seller (PENDING_REVIEW, ACTIVE, SUSPENDED). Valid transitions: PENDING_REVIEW -> ACTIVE, ACTIVE -> SUSPENDED, SUSPENDED -> ACTIVE",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Seller status updated successfully",
                    content = @Content(schema = @Schema(implementation = SellerResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Seller not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - invalid status transition",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Unprocessable - seller state invalid for operation",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<SellerResponse> updateSellerStatus(
            @Parameter(description = "Seller UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSellerStatusRequest request) {
        return ResponseEntity.ok(adminSellerService.updateSellerStatus(id, request));
    }

    @Operation(
            summary = "Review a seller document",
            description = "Reviews a seller's document (approve or reject) with optional notes",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document reviewed successfully",
                    content = @Content(schema = @Schema(implementation = SellerDocumentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Document not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - document already reviewed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/documents/{id}/review")
    public ResponseEntity<SellerDocumentResponse> reviewDocument(
            @Parameter(description = "Document UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @Valid @RequestBody ReviewDocumentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(adminSellerService.reviewDocument(id, request, jwt));
    }
}