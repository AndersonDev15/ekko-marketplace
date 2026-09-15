package com.ekko.product_service.controller;

import com.ekko.product_service.dto.request.InventoryQuantityRequest;
import com.ekko.product_service.dto.request.StockReservationItem;
import com.ekko.product_service.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SERVICE_ORDER')")
@Tag(name = "Internal Inventory", description = "Internal inventory endpoints for order service")
@SecurityRequirement(name = "bearerAuth")
public class InternalInventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "Reserve stock", description = "Reserves stock for multiple variants (internal use by order service)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock reserved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SERVICE_ORDER role"),
            @ApiResponse(responseCode = "409", description = "Conflict - insufficient stock")
    })
    @PostMapping("/reserve")
    public ResponseEntity<Void> reserve(
            @Parameter(description = "List of stock reservation items", required = true) @RequestBody List<StockReservationItem> items) {

        inventoryService.reserveStockBatch(items);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Confirm stock reservation", description = "Confirms a stock reservation, reducing available stock (internal use by order service)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock confirmed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SERVICE_ORDER role"),
            @ApiResponse(responseCode = "404", description = "Variant not found"),
            @ApiResponse(responseCode = "409", description = "Conflict - invalid stock operation")
    })
    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(
            @Parameter(description = "Inventory quantity request") @RequestBody InventoryQuantityRequest request) {

        inventoryService.confirmStock(request.variantId(), request.quantity());

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Release stock reservation", description = "Releases a stock reservation, increasing available stock (internal use by order service)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock released successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SERVICE_ORDER role"),
            @ApiResponse(responseCode = "404", description = "Variant not found"),
            @ApiResponse(responseCode = "409", description = "Conflict - invalid stock operation")
    })
    @PostMapping("/release")
    public ResponseEntity<Void> release(
            @Parameter(description = "Inventory quantity request") @RequestBody InventoryQuantityRequest request) {

        inventoryService.releaseStock(request.variantId(), request.quantity());

        return ResponseEntity.ok().build();
    }
}