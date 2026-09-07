package com.ekko.review_service.controller;

import com.ekko.review_service.dto.response.ReviewImageResponse;
import com.ekko.review_service.service.ReviewImageService;
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
public class ReviewImageController {

    private final ReviewImageService reviewImageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReviewImageResponse> uploadImage(
            @PathVariable UUID reviewId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        ReviewImageResponse response =
                reviewImageService.uploadImage(reviewId, file, jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> deleteImage(
            @PathVariable UUID reviewId,
            @PathVariable UUID imageId,
            @AuthenticationPrincipal Jwt jwt) {
        reviewImageService.deleteImage(reviewId, imageId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
