package com.ekko.product_service.controller;

import com.ekko.product_service.dto.request.AdjustStockRequest;
import com.ekko.product_service.dto.request.InventoryQuantityRequest;
import com.ekko.product_service.dto.response.InventoryViewResponse;
import com.ekko.product_service.service.InventoryService;
import com.ekko.product_service.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/seller/variants/{variantId}/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class InventoryController {

    private final InventoryService inventoryService;




    @GetMapping
    public ResponseEntity<InventoryViewResponse> getInventory(
            @PathVariable UUID variantId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                inventoryService.getInventory(variantId, JwtUtils.keycloakId(jwt)));
    }

    @PutMapping
    public ResponseEntity<InventoryViewResponse> adjustStock(
            @PathVariable UUID variantId,
            @RequestBody AdjustStockRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        InventoryViewResponse response =
                inventoryService.adjustStock(variantId, request.newStock(), JwtUtils.keycloakId(jwt));
        return ResponseEntity.ok(response);
    }
}