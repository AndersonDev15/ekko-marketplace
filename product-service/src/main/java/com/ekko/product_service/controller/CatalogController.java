package com.ekko.product_service.controller;

import com.ekko.product_service.dto.request.ProductFiltersRequest;
import com.ekko.product_service.dto.response.ProductDetailResponse;
import com.ekko.product_service.dto.response.ProductSummaryResponse;
import com.ekko.product_service.enums.ProductSortOption;
import com.ekko.product_service.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Catalog", description = "Public catalog search and product detail endpoints")
public class CatalogController {

    private final CatalogService catalogService;

    @Operation(summary = "Search products", description = "Search products with various filters and sorting options")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    @GetMapping
    public ResponseEntity<Page<ProductSummaryResponse>> searchProducts(
            @Parameter(description = "Filter by category ID") @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "Filter by brand ID") @RequestParam(required = false) UUID brandId,
            @Parameter(description = "Filter by seller ID") @RequestParam(required = false) UUID sellerId,
            @Parameter(description = "Minimum price filter") @RequestParam(required = false) @PositiveOrZero BigDecimal minPrice,
            @Parameter(description = "Maximum price filter") @RequestParam(required = false) @PositiveOrZero BigDecimal maxPrice,
            @Parameter(description = "Search query text") @RequestParam(required = false) String query,
            @Parameter(description = "Page number (0-based)", example = "0") @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @Parameter(description = "Page size (1-100)", example = "20") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Sort option", example = "RECENT") @RequestParam(defaultValue = "RECENT") ProductSortOption sort) {

        ProductFiltersRequest filters = new ProductFiltersRequest(
                categoryId, brandId, sellerId, minPrice, maxPrice, query);
        return ResponseEntity.ok(catalogService.searchProducts(filters, page, size, sort));
    }

    @Operation(summary = "Get product detail by slug", description = "Returns detailed product information for a product slug")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response",
                    content = @Content(schema = @Schema(implementation = ProductDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{slug}")
    public ResponseEntity<ProductDetailResponse> getProductDetail(
            @Parameter(description = "Product slug identifier", example = "awesome-product") @PathVariable String slug) {
        return ResponseEntity.ok(catalogService.getProductDetail(slug));
    }
}