package com.ekko.product_service.controller;

import com.ekko.product_service.dto.request.ReorderImagesRequest;
import com.ekko.product_service.dto.response.ProductImageResponse;
import com.ekko.product_service.service.ImageService;
import com.ekko.product_service.util.JwtUtils;
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
public class ImageController {

    private final ImageService imageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductImageResponse> uploadImage(
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "isPrimary", required = false) Boolean isPrimary,
            @AuthenticationPrincipal Jwt jwt) {
        ProductImageResponse response =
                imageService.uploadImage(id, file, isPrimary, JwtUtils.keycloakId(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable UUID id,
            @PathVariable UUID imageId,
            @AuthenticationPrincipal Jwt jwt) {
        imageService.deleteImage(id, imageId, JwtUtils.keycloakId(jwt));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{imageId}/primary")
    public ResponseEntity<Void> setPrimaryImage(
            @PathVariable UUID id,
            @PathVariable UUID imageId,
            @AuthenticationPrincipal Jwt jwt) {
        imageService.setPrimaryImage(id, imageId, JwtUtils.keycloakId(jwt));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/reorder")
    public ResponseEntity<Void> reorderImages(
            @PathVariable UUID id,
            @RequestBody ReorderImagesRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        imageService.reorderImages(id, request.imageIds(), JwtUtils.keycloakId(jwt));
        return ResponseEntity.noContent().build();
    }
}
