package com.ekko.seller_service.controller;

import com.ekko.seller_service.dto.request.SellerDocumentRequest;
import com.ekko.seller_service.dto.response.SellerDocumentResponse;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.support.SellerResolver;
import com.ekko.seller_service.service.SellerDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sellers/documents")
@PreAuthorize("hasRole('SELLER')")
public class SellerDocumentController {

    private final SellerDocumentService sellerDocumentService;
    private final SellerResolver sellerResolver;

    @PostMapping
    public ResponseEntity<SellerDocumentResponse> addDocument(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid SellerDocumentRequest request
    ) {
        Seller seller = sellerResolver.resolve(jwt.getSubject());

        return ResponseEntity.ok(
                sellerDocumentService.addDocument(seller.getId(), request)
        );
    }

    @GetMapping
    public ResponseEntity<List<SellerDocumentResponse>> getMyDocuments(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Seller seller = sellerResolver.resolve(jwt.getSubject());

        return ResponseEntity.ok(
                sellerDocumentService.getMyDocuments(seller.getId())
        );
    }
}