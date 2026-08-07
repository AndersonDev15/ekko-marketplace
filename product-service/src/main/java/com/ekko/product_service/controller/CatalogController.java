package com.ekko.product_service.controller;

import com.ekko.product_service.dto.request.ProductFiltersRequest;
import com.ekko.product_service.dto.response.ProductDetailResponse;
import com.ekko.product_service.dto.response.ProductSummaryResponse;
import com.ekko.product_service.enums.ProductSortOption;
import com.ekko.product_service.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping
    public ResponseEntity<Page<ProductSummaryResponse>> search(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) UUID sellerId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "RECENT") ProductSortOption sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ProductFiltersRequest filters = new ProductFiltersRequest(
                categoryId, brandId, sellerId, minPrice, maxPrice, query);

        return ResponseEntity.ok(catalogService.searchProducts(filters, page, size, sort));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ProductDetailResponse> getProduct(@PathVariable String slug) {
        return ResponseEntity.ok(catalogService.getProductDetail(slug));
    }
}