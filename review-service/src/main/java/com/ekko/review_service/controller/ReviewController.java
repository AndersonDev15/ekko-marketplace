package com.ekko.review_service.controller;

import com.ekko.review_service.service.HelpfulVoteService;
import com.ekko.review_service.service.ReviewCommandService;
import com.ekko.review_service.service.ReviewQueryService;
import com.ekko.review_service.dto.request.CreateReviewRequest;
import com.ekko.review_service.dto.response.EligibleToReviewResponse;
import com.ekko.review_service.dto.response.ReviewResponse;
import com.ekko.review_service.dto.request.UpdateReviewRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewCommandService reviewCommandService;
    private final ReviewQueryService reviewQueryService;
    private final HelpfulVoteService helpfulVoteService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ReviewResponse review = reviewCommandService.createReview(request, keycloakId(jwt).toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    @PatchMapping("/{reviewId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable UUID reviewId,
            @Valid @RequestBody UpdateReviewRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ReviewResponse review = reviewCommandService.updateReview(reviewId, request, keycloakId(jwt).toString());
        return ResponseEntity.ok(review);
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> deleteReview(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal Jwt jwt) {
        reviewCommandService.deleteReview(reviewId, keycloakId(jwt).toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> getReviewById(@PathVariable UUID reviewId) {
        return ResponseEntity.ok(reviewQueryService.getReviewById(reviewId));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ReviewResponse> reviews = reviewQueryService.getMyReviews(keycloakId(jwt).toString(), pageable);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/eligible")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<EligibleToReviewResponse>> getEligibleToReview(
            @AuthenticationPrincipal Jwt jwt) {
        List<EligibleToReviewResponse> eligible = reviewQueryService.getEligibleToReview(keycloakId(jwt).toString());
        return ResponseEntity.ok(eligible);
    }

    @PostMapping("/{reviewId}/helpful-votes")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> addHelpfulVote(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal Jwt jwt) {
        helpfulVoteService.addVote(reviewId, keycloakId(jwt).toString());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{reviewId}/helpful-votes")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> removeHelpfulVote(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal Jwt jwt) {
        helpfulVoteService.removeVote(reviewId, keycloakId(jwt).toString());
        return ResponseEntity.noContent().build();
    }

    private UUID keycloakId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}