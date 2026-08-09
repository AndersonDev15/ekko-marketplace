package com.ekko.product_service.controller;

import com.ekko.product_service.dto.request.InventoryQuantityRequest;
import com.ekko.product_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/inventory")
@RequiredArgsConstructor
public class InternalInventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/reserve")
    public ResponseEntity<Void> reserve(
            @RequestBody InventoryQuantityRequest request) {

        inventoryService.reserveStock(
                request.variantId(),
                request.quantity()
        );

        return ResponseEntity.ok().build();
    }
}