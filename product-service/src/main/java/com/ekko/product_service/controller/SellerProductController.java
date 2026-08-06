package com.ekko.product_service.controller;

import com.ekko.product_service.dto.request.CreateProductRequest;
import com.ekko.product_service.dto.request.UpdateProductRequest;
import com.ekko.product_service.dto.response.ProductDetailResponse;
import com.ekko.product_service.dto.response.ProductResponse;
import com.ekko.product_service.service.ProductService;
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
public class SellerProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ProductResponse response =
                productService.createProduct(request, keycloakId(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getMyProducts(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.getMyProducts(keycloakId(jwt), page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> getProductById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(productService.getProductById(id, keycloakId(jwt)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable UUID id,
            @RequestBody UpdateProductRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(productService.updateProduct(id, request, keycloakId(jwt)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDeleteProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        productService.softDeleteProduct(id, keycloakId(jwt));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<Void> submitForReview(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        productService.submitForReview(id, keycloakId(jwt));
        return ResponseEntity.noContent().build();
    }

    private UUID keycloakId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}