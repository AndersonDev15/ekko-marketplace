package com.ekko.review_service.controller;

import com.ekko.review_service.service.ReviewQueryService;
import com.ekko.review_service.dto.response.ProductReviewsResponse;
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
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductReviewsController {

    private final ReviewQueryService reviewQueryService;

    @GetMapping("/{productId}/reviews")
    public ResponseEntity<ProductReviewsResponse> getProductReviews(
            @PathVariable UUID productId,
            @PageableDefault(size = 20) Pageable pageable) {
        ProductReviewsResponse reviews = reviewQueryService.getProductReviews(productId, pageable);
        return ResponseEntity.ok(reviews);
    }
}