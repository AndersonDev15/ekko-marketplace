package com.ekko.review_service.controller;

import com.ekko.review_service.dto.response.ReviewImageResponse;
import com.ekko.review_service.service.ReviewImageService;
import com.ekko.review_service.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/reviews/{reviewId}/images")
@RequiredArgsConstructor
@Tag(name = "Review Images", description = "Endpoints for managing review images")
public class ReviewImageController {

    private final ReviewImageService reviewImageService;

    @Operation(
            summary = "Upload a review image",
            description = "Uploads an image for a review. Requires CUSTOMER role and ownership of the review. Multipart/form-data with 'file' part.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Image uploaded successfully",
                    content = @Content(schema = @Schema(implementation = ReviewImageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or image limit exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires CUSTOMER role or ownership violation",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Review not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Image upload failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReviewImageResponse> uploadImage(
            @Parameter(description = "Review ID", required = true)
            @PathVariable UUID reviewId,
            @Parameter(description = "Image file", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        ReviewImageResponse response =
                reviewImageService.uploadImage(reviewId, file, jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Delete a review image",
            description = "Deletes an image from a review. Requires CUSTOMER role and ownership of the review.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Image deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires CUSTOMER role or ownership violation",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Review or image not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> deleteImage(
            @Parameter(description = "Review ID", required = true)
            @PathVariable UUID reviewId,
            @Parameter(description = "Image ID", required = true)
            @PathVariable UUID imageId,
            @AuthenticationPrincipal Jwt jwt) {
        reviewImageService.deleteImage(reviewId, imageId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
