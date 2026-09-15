package com.ekko.review_service.controller;

import com.ekko.review_service.service.ReviewQueryService;
import com.ekko.review_service.dto.response.ProductReviewsResponse;
import com.ekko.review_service.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/product-reviews")
@RequiredArgsConstructor
@Tag(name = "Product Reviews", description = "Public endpoints for retrieving product reviews and ratings")
public class ProductReviewsController {

    private final ReviewQueryService reviewQueryService;

    @Operation(
            summary = "Get product reviews",
            description = "Returns paginated reviews for a product with average rating and rating distribution. Public endpoint - no authentication required."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product reviews retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ProductReviewsResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{productId}/reviews")
    public ResponseEntity<ProductReviewsResponse> getProductReviews(
            @Parameter(description = "Product ID", required = true)
            @PathVariable UUID productId,
            @Parameter(hidden = true)
            @PageableDefault(size = 20) Pageable pageable) {
        ProductReviewsResponse reviews = reviewQueryService.getProductReviews(productId, pageable);
        return ResponseEntity.ok(reviews);
    }
}