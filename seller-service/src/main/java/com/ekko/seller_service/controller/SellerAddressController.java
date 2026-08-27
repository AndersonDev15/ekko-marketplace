package com.ekko.seller_service.controller;

import com.ekko.seller_service.dto.request.SellerAddressRequest;
import com.ekko.seller_service.dto.response.SellerAddressResponse;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.support.SellerResolver;
import com.ekko.seller_service.service.SellerAddressService;
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
@RequestMapping("/sellers/addresses")
@PreAuthorize("hasRole('SELLER')")
public class SellerAddressController {

    private final SellerAddressService addressService;
    private final SellerResolver sellerResolver;

    @PostMapping
    public ResponseEntity<SellerAddressResponse> addAddress(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid SellerAddressRequest request
    ) {
        Seller seller = sellerResolver.resolve(jwt.getSubject());

        return ResponseEntity.ok(
                addressService.addAddress(seller.getId(), request)
        );
    }

    @GetMapping
    public ResponseEntity<List<SellerAddressResponse>> getMyAddresses(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Seller seller = sellerResolver.resolve(jwt.getSubject());

        return ResponseEntity.ok(
                addressService.getMyAddresses(seller.getId())
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<SellerAddressResponse> updateAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody @Valid SellerAddressRequest request
    ) {
        Seller seller = sellerResolver.resolve(jwt.getSubject());

        return ResponseEntity.ok(
                addressService.updateAddress(seller.getId(), id, request)
        );
    }

    @PatchMapping("/{addressId}/primary")
    public ResponseEntity<SellerAddressResponse> setPrimaryAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID addressId) {
        Seller seller = sellerResolver.resolve(jwt.getSubject());
        return ResponseEntity.ok(addressService.setPrimaryAddress(seller.getId(), addressId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        Seller seller = sellerResolver.resolve(jwt.getSubject());

        addressService.deleteAddress(seller.getId(), id);

        return ResponseEntity.noContent().build();
    }
}