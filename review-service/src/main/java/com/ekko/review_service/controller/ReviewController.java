package com.ekko.review_service.controller;

import com.ekko.review_service.service.HelpfulVoteService;
import com.ekko.review_service.service.ReviewCommandService;
import com.ekko.review_service.service.ReviewQueryService;
import com.ekko.review_service.dto.request.CreateReviewRequest;
import com.ekko.review_service.dto.response.EligibleToReviewResponse;
import com.ekko.review_service.dto.response.ReviewResponse;
import com.ekko.review_service.dto.request.UpdateReviewRequest;
import com.ekko.review_service.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Reviews", description = "Endpoints for managing product reviews")
public class ReviewController {

    private final ReviewCommandService reviewCommandService;
    private final ReviewQueryService reviewQueryService;
    private final HelpfulVoteService helpfulVoteService;

    @Operation(
            summary = "Create a new review",
            description = "Creates a new review for a product. Requires CUSTOMER role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Review created successfully",
                    content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires CUSTOMER role or not eligible to review",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Review already exists for this order item",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ReviewResponse review = reviewCommandService.createReview(request, keycloakId(jwt).toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    @Operation(
            summary = "Update a review",
            description = "Updates an existing review. Requires CUSTOMER role and ownership of the review.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review updated successfully",
                    content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires CUSTOMER role, ownership, or edit window expired",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Review not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{reviewId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReviewResponse> updateReview(
            @Parameter(description = "Review ID", required = true)
            @PathVariable UUID reviewId,
            @Valid @RequestBody UpdateReviewRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ReviewResponse review = reviewCommandService.updateReview(reviewId, request, keycloakId(jwt).toString());
        return ResponseEntity.ok(review);
    }

    @Operation(
            summary = "Delete a review",
            description = "Deletes a review. Requires CUSTOMER role and ownership of the review.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Review deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires CUSTOMER role or ownership violation",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Review not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> deleteReview(
            @Parameter(description = "Review ID", required = true)
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal Jwt jwt) {
        reviewCommandService.deleteReview(reviewId, keycloakId(jwt).toString());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get a review by ID",
            description = "Returns a single review by its ID. Public endpoint - no authentication required."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review found",
                    content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
            @ApiResponse(responseCode = "404", description = "Review not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> getReviewById(
            @Parameter(description = "Review ID", required = true)
            @PathVariable UUID reviewId) {
        return ResponseEntity.ok(reviewQueryService.getReviewById(reviewId));
    }

    @Operation(
            summary = "Get my reviews",
            description = "Returns paginated list of reviews created by the authenticated customer.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reviews retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires CUSTOMER role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(hidden = true)
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ReviewResponse> reviews = reviewQueryService.getMyReviews(keycloakId(jwt).toString(), pageable);
        return ResponseEntity.ok(reviews);
    }

    @Operation(
            summary = "Get eligible orders to review",
            description = "Returns list of order items that the authenticated customer is eligible to review.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Eligible items retrieved successfully",
                    content = @Content(schema = @Schema(implementation = EligibleToReviewResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires CUSTOMER role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/eligible")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<EligibleToReviewResponse>> getEligibleToReview(
            @AuthenticationPrincipal Jwt jwt) {
        List<EligibleToReviewResponse> eligible = reviewQueryService.getEligibleToReview(keycloakId(jwt).toString());
        return ResponseEntity.ok(eligible);
    }

    @Operation(
            summary = "Add a helpful vote",
            description = "Adds a helpful vote to a review. Requires CUSTOMER role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Vote added successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires CUSTOMER role or self-vote not allowed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Review not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Vote already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{reviewId}/helpful-votes")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> addHelpfulVote(
            @Parameter(description = "Review ID", required = true)
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal Jwt jwt) {
        helpfulVoteService.addVote(reviewId, keycloakId(jwt).toString());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Remove a helpful vote",
            description = "Removes a helpful vote from a review. Requires CUSTOMER role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Vote removed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires CUSTOMER role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Vote not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{reviewId}/helpful-votes")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> removeHelpfulVote(
            @Parameter(description = "Review ID", required = true)
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal Jwt jwt) {
        helpfulVoteService.removeVote(reviewId, keycloakId(jwt).toString());
        return ResponseEntity.noContent().build();
    }

    private UUID keycloakId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}