package com.ekko.seller_service.controller;

import com.ekko.seller_service.dto.request.ReviewDocumentRequest;
import com.ekko.seller_service.dto.request.UpdateSellerStatusRequest;
import com.ekko.seller_service.dto.response.SellerDetailResponse;
import com.ekko.seller_service.dto.response.SellerDocumentResponse;
import com.ekko.seller_service.dto.response.SellerResponse;
import com.ekko.seller_service.dto.response.SellerSummaryResponse;
import com.ekko.seller_service.enums.SellerStatus;
import com.ekko.seller_service.service.AdminSellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/sellers/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSellerController {

    private final AdminSellerService adminSellerService;

    @GetMapping
    public ResponseEntity<Page<SellerSummaryResponse>> getAllSellers(
            @RequestParam(required = false) SellerStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable) {
        return ResponseEntity.ok(adminSellerService.getAllSellers(status, from, to, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SellerDetailResponse> getSellerDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(adminSellerService.getSellerDetail(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<SellerResponse> updateSellerStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSellerStatusRequest request) {
        return ResponseEntity.ok(adminSellerService.updateSellerStatus(id, request));
    }

    @PutMapping("/documents/{id}/review")
    public ResponseEntity<SellerDocumentResponse> reviewDocument(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewDocumentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(adminSellerService.reviewDocument(id, request, jwt));
    }
}