package com.ekko.seller_service.controller;

import com.ekko.seller_service.dto.response.DocumentDownloadResponse;
import com.ekko.seller_service.dto.response.ErrorResponse;
import com.ekko.seller_service.dto.response.SellerDocumentResponse;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.enums.DocumentType;
import com.ekko.seller_service.support.SellerResolver;
import com.ekko.seller_service.service.SellerDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Seller Documents", description = "Endpoints for managing seller documents")
public class SellerDocumentController {

    private final SellerDocumentService sellerDocumentService;
    private final SellerResolver sellerResolver;

    @Operation(
            summary = "Upload a seller document",
            description = "Uploads a document (ID card, RUT, business license, or bank certificate) for the authenticated seller",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document uploaded successfully",
                    content = @Content(schema = @Schema(implementation = SellerDocumentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file, missing parameters, or document already pending/approved",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - document already pending, approved, or reviewed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "Bad Gateway - failed to upload to storage",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SellerDocumentResponse> uploadDocument(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Document file to upload", required = true)
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "Type of document", required = true, schema = @Schema(implementation = DocumentType.class))
            @RequestParam("documentType") DocumentType documentType
    ) {
        Seller seller = sellerResolver.resolve(jwt.getSubject());

        return ResponseEntity.ok(
                sellerDocumentService.uploadDocument(seller.getId(), documentType, file)
        );
    }

    @Operation(
            summary = "Get my documents",
            description = "Returns all documents uploaded by the authenticated seller",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documents retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SellerDocumentResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<SellerDocumentResponse>> getMyDocuments(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Seller seller = sellerResolver.resolve(jwt.getSubject());

        return ResponseEntity.ok(
                sellerDocumentService.getMyDocuments(seller.getId())
        );
    }

    @Operation(
            summary = "Get document download URL",
            description = "Returns a presigned URL to download a specific document",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Download URL generated successfully",
                    content = @Content(schema = @Schema(implementation = DocumentDownloadResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Document not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/download")
    public ResponseEntity<DocumentDownloadResponse> downloadDocument(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Document UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id
    ) {

        Seller seller = sellerResolver.resolve(jwt.getSubject());
        return ResponseEntity.ok(
                sellerDocumentService.getDownloadUrl(seller.getId(), id)
        );
    }
}