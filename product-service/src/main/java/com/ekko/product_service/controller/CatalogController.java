package com.ekko.product_service.controller;

import com.ekko.product_service.dto.request.ProductFiltersRequest;
import com.ekko.product_service.dto.response.ProductDetailResponse;
import com.ekko.product_service.dto.response.ProductSummaryResponse;
import com.ekko.product_service.enums.ProductSortOption;
import com.ekko.product_service.service.CatalogService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/catalog/products")
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping
    public ResponseEntity<Page<ProductSummaryResponse>> searchProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) UUID sellerId,
            @RequestParam(required = false) @PositiveOrZero BigDecimal minPrice,
            @RequestParam(required = false) @PositiveOrZero BigDecimal maxPrice,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "RECENT") ProductSortOption sort) {

        ProductFiltersRequest filters = new ProductFiltersRequest(
                categoryId, brandId, sellerId, minPrice, maxPrice, query);
        return ResponseEntity.ok(catalogService.searchProducts(filters, page, size, sort));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ProductDetailResponse> getProductDetail(@PathVariable String slug) {
        return ResponseEntity.ok(catalogService.getProductDetail(slug));
    }
}