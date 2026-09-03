package com.ekko.product_service.controller;

import com.ekko.product_service.dto.request.RejectProductRequest;
import com.ekko.product_service.dto.response.ProductResponse;
import com.ekko.product_service.enums.ProductStatus;
import com.ekko.product_service.service.AdminProductService;
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
public class AdminProductController {

    private final AdminProductService adminProductService;

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                adminProductService.getAllProducts(status, page, size)
        );
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<Void> approveProduct(@PathVariable UUID id) {
        adminProductService.approveProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<Void> rejectProduct(
            @PathVariable UUID id,
            @RequestBody RejectProductRequest request) {
        adminProductService.rejectProduct(id, request.reason());
        return ResponseEntity.noContent().build();
    }
}