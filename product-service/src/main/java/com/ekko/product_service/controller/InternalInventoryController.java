package com.ekko.product_service.controller;

import com.ekko.product_service.dto.request.InventoryQuantityRequest;
import com.ekko.product_service.dto.request.StockReservationItem;
import com.ekko.product_service.service.InventoryService;
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
public class InternalInventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/reserve")
    public ResponseEntity<Void> reserve(
            @RequestBody List<StockReservationItem> items) {

        inventoryService.reserveStockBatch(items);

        return ResponseEntity.ok().build();
    }
}