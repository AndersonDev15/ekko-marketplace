package com.ekko.seller_service.controller;

import com.ekko.seller_service.dto.response.DocumentDownloadResponse;
import com.ekko.seller_service.dto.response.SellerDocumentResponse;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.enums.DocumentType;
import com.ekko.seller_service.support.SellerResolver;
import com.ekko.seller_service.service.SellerDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sellers/documents")
@PreAuthorize("hasRole('SELLER')")
public class SellerDocumentController {

    private final SellerDocumentService sellerDocumentService;
    private final SellerResolver sellerResolver;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SellerDocumentResponse> uploadDocument(
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart("file") MultipartFile file,
            @RequestParam("documentType") DocumentType documentType
    ) {
        Seller seller = sellerResolver.resolve(jwt.getSubject());

        return ResponseEntity.ok(
                sellerDocumentService.uploadDocument(seller.getId(), documentType, file)
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

    @GetMapping("/{id}/download")
    public ResponseEntity<DocumentDownloadResponse> downloadDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {

        Seller seller = sellerResolver.resolve(jwt.getSubject());
return ResponseEntity.ok(
                sellerDocumentService.getDownloadUrl(seller.getId(), id)
        );
    }
}