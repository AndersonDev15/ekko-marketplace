package com.ekko.product_service.controller;

import com.ekko.product_service.dto.response.ProductCatalogResponse;
import com.ekko.product_service.dto.response.ProductDetailResponse;
import com.ekko.product_service.service.PublicProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class PublicProductController {

    private final PublicProductService publicProductService;

    @GetMapping
    public ResponseEntity<Page<ProductCatalogResponse>> getCatalog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(publicProductService.getPublicCatalog(page, size));
    }

    @GetMapping("/stores/{sellerSlug}/{productSlug}")
    public ResponseEntity<ProductDetailResponse> getProductByStoreAndSlug(
            @PathVariable String sellerSlug,
            @PathVariable String productSlug) {
        return ResponseEntity.ok(publicProductService.getPublicProduct(sellerSlug, productSlug));
    }
}