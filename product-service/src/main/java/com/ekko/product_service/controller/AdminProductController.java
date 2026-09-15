package com.ekko.product_service.controller;

import com.ekko.product_service.dto.request.RejectProductRequest;
import com.ekko.product_service.dto.response.ProductResponse;
import com.ekko.product_service.enums.ProductStatus;
import com.ekko.product_service.service.AdminProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Products", description = "Product management endpoints for administrators")
@SecurityRequirement(name = "bearerAuth")
public class AdminProductController {

    private final AdminProductService adminProductService;

    @Operation(summary = "Get all products (admin)", description = "Returns a paginated list of all products with optional status filter")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @Parameter(description = "Filter by product status") @RequestParam(required = false) ProductStatus status,
            @Parameter(description = "Page number (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                adminProductService.getAllProducts(status, page, size)
        );
    }

    @Operation(summary = "Approve product", description = "Approves a product, changing its status to ACTIVE")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product approved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "409", description = "Conflict - invalid product status for approval")
    })
    @PatchMapping("/{id}/approve")
    public ResponseEntity<Void> approveProduct(
            @Parameter(description = "Product ID") @PathVariable UUID id) {
        adminProductService.approveProduct(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reject product", description = "Rejects a product with a reason, changing its status to REJECTED")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product rejected successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "409", description = "Conflict - invalid product status for rejection")
    })
    @PatchMapping("/{id}/reject")
    public ResponseEntity<Void> rejectProduct(
            @Parameter(description = "Product ID") @PathVariable UUID id,
            @Parameter(description = "Rejection request with reason") @RequestBody RejectProductRequest request) {
        adminProductService.rejectProduct(id, request.reason());
        return ResponseEntity.noContent().build();
    }
}