package com.ekko.seller_service.controller;

import com.ekko.seller_service.dto.request.SellerBankAccountRequest;
import com.ekko.seller_service.dto.response.SellerBankAccountResponse;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.support.SellerResolver;
import com.ekko.seller_service.service.SellerBankAccountService;
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
public class SellerBankAccountController {

    private final SellerBankAccountService bankAccountService;
    private final SellerResolver sellerResolver;

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

    @PutMapping("/{id}")
    public ResponseEntity<SellerBankAccountResponse> updateBankAccount(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody @Valid SellerBankAccountRequest request
    ) {
        Seller seller = sellerResolver.resolve(jwt.getSubject());

        return ResponseEntity.ok(
                bankAccountService.updateBankAccount(seller.getId(), id, request)
        );
    }

    @PatchMapping("/{accountId}/primary")
    public ResponseEntity<SellerBankAccountResponse> setPrimaryBankAccount(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID accountId) {
        Seller seller = sellerResolver.resolve(jwt.getSubject());
        return ResponseEntity.ok(bankAccountService.setPrimaryBankAccount(seller.getId(), accountId));
    }

    @GetMapping
    public ResponseEntity<List<SellerBankAccountResponse>> getMyBankAccounts(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Seller seller = sellerResolver.resolve(jwt.getSubject());

        return ResponseEntity.ok(
                bankAccountService.getMyBankAccounts(seller.getId())
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBankAccount(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        Seller seller = sellerResolver.resolve(jwt.getSubject());

        bankAccountService.deleteBankAccount(seller.getId(), id);

        return ResponseEntity.noContent().build();
    }
}