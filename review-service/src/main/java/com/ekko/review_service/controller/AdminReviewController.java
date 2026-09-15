package com.ekko.review_service.controller;

import com.ekko.review_service.enums.ReviewStatus;
import com.ekko.review_service.service.ReviewCommandService;
import com.ekko.review_service.service.ReviewQueryService;
import com.ekko.review_service.dto.request.AdminUpdateContentRequest;
import com.ekko.review_service.dto.response.ReviewResponse;
import com.ekko.review_service.dto.request.UpdateReviewStatusRequest;
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
@Tag(name = "Admin Reviews", description = "Admin endpoints for managing reviews")
public class AdminReviewController {

    private final ReviewCommandService reviewCommandService;
    private final ReviewQueryService reviewQueryService;

    @Operation(
            summary = "Get all reviews (admin)",
            description = "Returns paginated list of all reviews with optional filters. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reviews retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<Page<ReviewResponse>> getAllReviews(
            @Parameter(description = "Filter by review status")
            @RequestParam(required = false) ReviewStatus status,
            @Parameter(description = "Filter by product ID")
            @RequestParam(required = false) UUID productId,
            @Parameter(description = "Filter by customer ID")
            @RequestParam(required = false) String customerId,
            @Parameter(hidden = true)
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ReviewResponse> reviews = reviewQueryService.adminGetAllReviews(status, productId, customerId, pageable);
        return ResponseEntity.ok(reviews);
    }

    @Operation(
            summary = "Update review status (admin)",
            description = "Updates the status of a review (VISIBLE/HIDDEN). Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review status updated successfully",
                    content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Review not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{reviewId}/status")
    public ResponseEntity<ReviewResponse> updateReviewStatus(
            @Parameter(description = "Review ID", required = true)
            @PathVariable UUID reviewId,
            @Valid @RequestBody UpdateReviewStatusRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ReviewResponse review = reviewCommandService.adminUpdateStatus(
                reviewId, request.newStatus(), keycloakId(jwt).toString());
        return ResponseEntity.ok(review);
    }

    @Operation(
            summary = "Update review content (admin)",
            description = "Updates the content of a review (rating, title, comment). Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review content updated successfully",
                    content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Review not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{reviewId}/content")
    public ResponseEntity<ReviewResponse> updateReviewContent(
            @Parameter(description = "Review ID", required = true)
            @PathVariable UUID reviewId,
            @Valid @RequestBody AdminUpdateContentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ReviewResponse review = reviewCommandService.adminUpdateContent(
                reviewId, request, keycloakId(jwt).toString());
        return ResponseEntity.ok(review);
    }

    @Operation(
            summary = "Delete a review (admin)",
            description = "Deletes a review. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Review deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Review not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @Parameter(description = "Review ID", required = true)
            @PathVariable UUID reviewId) {
        reviewCommandService.adminDeleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    private UUID keycloakId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}