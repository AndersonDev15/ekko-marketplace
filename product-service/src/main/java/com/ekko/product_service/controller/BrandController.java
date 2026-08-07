package com.ekko.product_service.controller;

import com.ekko.product_service.dto.request.CreateBrandRequest;
import com.ekko.product_service.dto.request.UpdateBrandRequest;
import com.ekko.product_service.dto.response.BrandResponse;
import com.ekko.product_service.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @GetMapping("/brands")
    public ResponseEntity<List<BrandResponse>> getActiveBrands() {
        return ResponseEntity.ok(brandService.getActiveBrands());
    }

    @PostMapping("/admin/brands")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BrandResponse> createBrand(@RequestBody CreateBrandRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(brandService.createBrand(request));
    }

    @PutMapping("/admin/brands/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BrandResponse> updateBrand(
            @PathVariable UUID id,
            @RequestBody UpdateBrandRequest request) {
        return ResponseEntity.ok(brandService.updateBrand(id, request));
    }

    @PatchMapping("/admin/brands/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateBrand(@PathVariable UUID id) {
        brandService.deactivateBrand(id);
        return ResponseEntity.noContent().build();
    }
}