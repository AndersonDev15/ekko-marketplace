package com.ekko.product_service.controller;

import com.ekko.product_service.dto.request.AdjustStockRequest;
import com.ekko.product_service.dto.request.InventoryQuantityRequest;
import com.ekko.product_service.dto.response.InventoryViewResponse;
import com.ekko.product_service.service.InventoryService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/seller/variants/{variantId}/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
@Tag(name = "Seller Inventory", description = "Inventory management endpoints for sellers")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "Get inventory", description = "Returns inventory details for a variant owned by the authenticated seller")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response",
                    content = @Content(schema = @Schema(implementation = InventoryViewResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role"),
            @ApiResponse(responseCode = "404", description = "Variant or inventory not found or access denied")
    })
    @GetMapping
    public ResponseEntity<InventoryViewResponse> getInventory(
            @Parameter(description = "Variant ID") @PathVariable UUID variantId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                inventoryService.getInventory(variantId, JwtUtils.keycloakId(jwt)));
    }

    @Operation(summary = "Adjust stock", description = "Adjusts the stock quantity for a variant owned by the authenticated seller")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock adjusted successfully",
                    content = @Content(schema = @Schema(implementation = InventoryViewResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid stock adjustment"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role"),
            @ApiResponse(responseCode = "404", description = "Variant or inventory not found or access denied"),
            @ApiResponse(responseCode = "409", description = "Conflict - invalid stock operation")
    })
    @PutMapping
    public ResponseEntity<InventoryViewResponse> adjustStock(
            @Parameter(description = "Variant ID") @PathVariable UUID variantId,
            @Parameter(description = "Stock adjustment request") @RequestBody AdjustStockRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        InventoryViewResponse response =
                inventoryService.adjustStock(variantId, request.newStock(), JwtUtils.keycloakId(jwt));
        return ResponseEntity.ok(response);
    }
}