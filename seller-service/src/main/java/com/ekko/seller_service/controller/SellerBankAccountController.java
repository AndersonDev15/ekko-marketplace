package com.ekko.seller_service.controller;

import com.ekko.seller_service.dto.request.SellerBankAccountRequest;
import com.ekko.seller_service.dto.response.ErrorResponse;
import com.ekko.seller_service.dto.response.SellerBankAccountResponse;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.support.SellerResolver;
import com.ekko.seller_service.service.SellerBankAccountService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sellers/bank-accounts")
@PreAuthorize("hasRole('SELLER')")
@Tag(name = "Seller Bank Accounts", description = "Endpoints for managing seller bank accounts")
public class SellerBankAccountController {

    private final SellerBankAccountService bankAccountService;
    private final SellerResolver sellerResolver;

    @Operation(
            summary = "Add a bank account",
            description = "Adds a new bank account for the authenticated seller",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bank account added successfully",
                    content = @Content(schema = @Schema(implementation = SellerBankAccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - duplicate bank account",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<SellerBankAccountResponse> addBankAccount(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid SellerBankAccountRequest request
    ) {
        Seller seller = sellerResolver.resolve(jwt.getSubject());

        return ResponseEntity.ok(
                bankAccountService.addBankAccount(seller.getId(), request)
        );
    }

    @Operation(
            summary = "Update a bank account",
            description = "Updates an existing bank account for the authenticated seller",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bank account updated successfully",
                    content = @Content(schema = @Schema(implementation = SellerBankAccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Bank account not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<SellerBankAccountResponse> updateBankAccount(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Bank account UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @RequestBody @Valid SellerBankAccountRequest request
    ) {
        Seller seller = sellerResolver.resolve(jwt.getSubject());

        return ResponseEntity.ok(
                bankAccountService.updateBankAccount(seller.getId(), id, request)
        );
    }

    @Operation(
            summary = "Set primary bank account",
            description = "Sets a bank account as the primary one for the authenticated seller",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Primary bank account set successfully",
                    content = @Content(schema = @Schema(implementation = SellerBankAccountResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Bank account not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{accountId}/primary")
    public ResponseEntity<SellerBankAccountResponse> setPrimaryBankAccount(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Bank account UUID to set as primary", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID accountId) {
        Seller seller = sellerResolver.resolve(jwt.getSubject());
        return ResponseEntity.ok(bankAccountService.setPrimaryBankAccount(seller.getId(), accountId));
    }

    @Operation(
            summary = "Get my bank accounts",
            description = "Returns all bank accounts for the authenticated seller",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bank accounts retrieved successfully",
                    content = @Content(schema = @Schema(implementation = SellerBankAccountResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<SellerBankAccountResponse>> getMyBankAccounts(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Seller seller = sellerResolver.resolve(jwt.getSubject());

        return ResponseEntity.ok(
                bankAccountService.getMyBankAccounts(seller.getId())
        );
    }

    @Operation(
            summary = "Delete a bank account",
            description = "Deletes a bank account for the authenticated seller. Cannot delete the last or primary account.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Bank account deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SELLER role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Bank account not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - cannot delete last or primary bank account",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBankAccount(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Bank account UUID to delete", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id
    ) {
        Seller seller = sellerResolver.resolve(jwt.getSubject());

        bankAccountService.deleteBankAccount(seller.getId(), id);

        return ResponseEntity.noContent().build();
    }
}