package com.ekko.product_service.controller;

import com.ekko.product_service.dto.request.CreateBrandRequest;
import com.ekko.product_service.dto.request.UpdateBrandRequest;
import com.ekko.product_service.dto.response.BrandResponse;
import com.ekko.product_service.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/admin/brands", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BrandResponse> createBrand(
            @RequestPart("data") CreateBrandRequest request,
            @RequestPart(value = "logo", required = false) MultipartFile logo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(brandService.createBrand(request, logo));
    }

    @PatchMapping(value = "/admin/brands/{brandId}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BrandResponse> updateBrandLogo(
            @PathVariable UUID brandId,
            @RequestPart("logo") MultipartFile logo) {
        return ResponseEntity.ok(brandService.updateLogo(brandId, logo));
    }

    @PutMapping("/admin/brands/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BrandResponse> updateBrand(
            @PathVariable UUID id,
            @RequestBody UpdateBrandRequest request) {
        return ResponseEntity.ok(brandService.updateBrand(id, request));
    }

    @DeleteMapping("/admin/brands/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBrand(@PathVariable UUID id) {
        brandService.deactivateBrand(id);
        return ResponseEntity.noContent().build();
    }
}