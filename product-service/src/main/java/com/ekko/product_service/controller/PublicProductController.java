package com.ekko.product_service.controller;

import com.ekko.product_service.dto.response.ProductCatalogResponse;
import com.ekko.product_service.dto.response.ProductDetailResponse;
import com.ekko.product_service.service.PublicProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Public Products", description = "Public product catalog endpoints")
public class PublicProductController {

    private final PublicProductService publicProductService;

    @Operation(summary = "Get public product catalog", description = "Returns a paginated list of active products for public catalog display")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response",
                    content = @Content(schema = @Schema(implementation = Page.class)))
    })
    @GetMapping
    public ResponseEntity<Page<ProductCatalogResponse>> getCatalog(
            @Parameter(description = "Page number (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(publicProductService.getPublicCatalog(page, size));
    }

    @Operation(summary = "Get product by store and product slug", description = "Returns detailed product information for a specific store and product slug")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response",
                    content = @Content(schema = @Schema(implementation = ProductDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/stores/{sellerSlug}/{productSlug}")
    public ResponseEntity<ProductDetailResponse> getProductByStoreAndSlug(
            @Parameter(description = "Seller slug identifier", example = "my-store") @PathVariable String sellerSlug,
            @Parameter(description = "Product slug identifier", example = "awesome-product") @PathVariable String productSlug) {
        return ResponseEntity.ok(publicProductService.getPublicProduct(sellerSlug, productSlug));
    }
}