package com.ekko.review_service.controller;

import com.ekko.review_service.enums.ReviewStatus;
import com.ekko.review_service.service.ReviewCommandService;
import com.ekko.review_service.service.ReviewQueryService;
import com.ekko.review_service.dto.request.AdminUpdateContentRequest;
import com.ekko.review_service.dto.response.ReviewResponse;
import com.ekko.review_service.dto.request.UpdateReviewStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewCommandService reviewCommandService;
    private final ReviewQueryService reviewQueryService;

    @GetMapping
    public ResponseEntity<Page<ReviewResponse>> getAllReviews(
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) String customerId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ReviewResponse> reviews = reviewQueryService.adminGetAllReviews(status, productId, customerId, pageable);
        return ResponseEntity.ok(reviews);
    }

    @PatchMapping("/{reviewId}/status")
    public ResponseEntity<ReviewResponse> updateReviewStatus(
            @PathVariable UUID reviewId,
            @Valid @RequestBody UpdateReviewStatusRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ReviewResponse review = reviewCommandService.adminUpdateStatus(
                reviewId, request.newStatus(), keycloakId(jwt).toString());
        return ResponseEntity.ok(review);
    }

    @PatchMapping("/{reviewId}/content")
    public ResponseEntity<ReviewResponse> updateReviewContent(
            @PathVariable UUID reviewId,
            @Valid @RequestBody AdminUpdateContentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ReviewResponse review = reviewCommandService.adminUpdateContent(
                reviewId, request, keycloakId(jwt).toString());
        return ResponseEntity.ok(review);
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable UUID reviewId) {
        reviewCommandService.adminDeleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    private UUID keycloakId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}