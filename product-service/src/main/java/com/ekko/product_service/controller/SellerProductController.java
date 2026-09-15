package com.ekko.product_service.controller;

import com.ekko.product_service.dto.request.CreateProductRequest;
import com.ekko.product_service.dto.request.UpdateProductRequest;
import com.ekko.product_service.dto.response.ProductDetailResponse;
import com.ekko.product_service.dto.response.ProductResponse;
import com.ekko.product_service.service.ProductService;
import com.ekko.product_service.util.JwtUtils;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/seller/products")
@PreAuthorize("hasRole('SELLER')")
@RequiredArgsConstructor
@Tag(name = "Seller Products", description = "Product management endpoints for sellers")
@SecurityRequirement(name = "bearerAuth")
public class SellerProductController {

    private final ProductService productService;

    @Operation(summary = "Create a new product", description = "Creates a new product for the authenticated seller")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role"),
            @ApiResponse(responseCode = "409", description = "Conflict - duplicate slug or other constraint violation")
    })
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Parameter(description = "Product creation request") @RequestBody CreateProductRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        ProductResponse response =
                productService.createProduct(request, JwtUtils.keycloakId(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get seller's products", description = "Returns a paginated list of products owned by the authenticated seller")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role")
    })
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getMyProducts(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Page number (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.getMyProducts(JwtUtils.keycloakId(jwt), page, size));
    }

    @Operation(summary = "Get product by ID", description = "Returns detailed product information for a product owned by the authenticated seller")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response",
                    content = @Content(schema = @Schema(implementation = ProductDetailResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role"),
            @ApiResponse(responseCode = "404", description = "Product not found or access denied")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> getProductById(
            @Parameter(description = "Product ID") @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(productService.getProductById(id, JwtUtils.keycloakId(jwt)));
    }

    @Operation(summary = "Update product", description = "Updates an existing product owned by the authenticated seller")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role"),
            @ApiResponse(responseCode = "404", description = "Product not found or access denied"),
            @ApiResponse(responseCode = "409", description = "Conflict - duplicate slug or other constraint violation")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @Parameter(description = "Product ID") @PathVariable UUID id,
            @Parameter(description = "Product update request") @RequestBody UpdateProductRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(productService.updateProduct(id, request, JwtUtils.keycloakId(jwt)));
    }

    @Operation(summary = "Soft delete product", description = "Soft deletes a product owned by the authenticated seller")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role"),
            @ApiResponse(responseCode = "404", description = "Product not found or access denied"),
            @ApiResponse(responseCode = "409", description = "Conflict - product already deleted")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDeleteProduct(
            @Parameter(description = "Product ID") @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        productService.softDeleteProduct(id, JwtUtils.keycloakId(jwt));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Submit product for review", description = "Submits a product for admin review to change status from DRAFT to PENDING_REVIEW")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product submitted for review successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role"),
            @ApiResponse(responseCode = "404", description = "Product not found or access denied"),
            @ApiResponse(responseCode = "409", description = "Conflict - invalid product status for submission")
    })
    @PostMapping("/{id}/submit")
    public ResponseEntity<Void> submitForReview(
            @Parameter(description = "Product ID") @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        productService.submitForReview(id, JwtUtils.keycloakId(jwt));
        return ResponseEntity.noContent().build();
    }
}