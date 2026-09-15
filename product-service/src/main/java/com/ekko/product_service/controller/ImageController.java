package com.ekko.product_service.controller;

import com.ekko.product_service.dto.request.ReorderImagesRequest;
import com.ekko.product_service.dto.response.ProductImageResponse;
import com.ekko.product_service.service.ImageService;
import com.ekko.product_service.util.JwtUtils;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/seller/products/{id}/images")
@PreAuthorize("hasRole('SELLER')")
@RequiredArgsConstructor
@Tag(name = "Seller Images", description = "Product image management endpoints for sellers")
@SecurityRequirement(name = "bearerAuth")
public class ImageController {

    private final ImageService imageService;

    @Operation(summary = "Upload product image", description = "Uploads an image for a product owned by the authenticated seller")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Image uploaded successfully",
                    content = @Content(schema = @Schema(implementation = ProductImageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role"),
            @ApiResponse(responseCode = "404", description = "Product not found or access denied"),
            @ApiResponse(responseCode = "502", description = "Image upload failed")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductImageResponse> uploadImage(
            @Parameter(description = "Product ID") @PathVariable UUID id,
            @Parameter(description = "Image file", required = true) @RequestPart("file") MultipartFile file,
            @Parameter(description = "Whether this image should be the primary image") @RequestParam(value = "isPrimary", required = false) Boolean isPrimary,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        ProductImageResponse response =
                imageService.uploadImage(id, file, isPrimary, JwtUtils.keycloakId(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Delete product image", description = "Deletes an image from a product owned by the authenticated seller")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Image deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role"),
            @ApiResponse(responseCode = "404", description = "Product or image not found or access denied"),
            @ApiResponse(responseCode = "409", description = "Conflict - cannot delete primary image")
    })
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @Parameter(description = "Product ID") @PathVariable UUID id,
            @Parameter(description = "Image ID") @PathVariable UUID imageId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        imageService.deleteImage(id, imageId, JwtUtils.keycloakId(jwt));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Set primary image", description = "Sets an image as the primary image for a product owned by the authenticated seller")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Primary image set successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role"),
            @ApiResponse(responseCode = "404", description = "Product or image not found or access denied")
    })
    @PatchMapping("/{imageId}/primary")
    public ResponseEntity<Void> setPrimaryImage(
            @Parameter(description = "Product ID") @PathVariable UUID id,
            @Parameter(description = "Image ID") @PathVariable UUID imageId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        imageService.setPrimaryImage(id, imageId, JwtUtils.keycloakId(jwt));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reorder images", description = "Reorders images for a product owned by the authenticated seller")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Images reordered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid image order"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role"),
            @ApiResponse(responseCode = "404", description = "Product not found or access denied")
    })
    @PatchMapping("/reorder")
    public ResponseEntity<Void> reorderImages(
            @Parameter(description = "Product ID") @PathVariable UUID id,
            @Parameter(description = "Reorder request with image IDs in new order") @RequestBody ReorderImagesRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        imageService.reorderImages(id, request.imageIds(), JwtUtils.keycloakId(jwt));
        return ResponseEntity.noContent().build();
    }
}
