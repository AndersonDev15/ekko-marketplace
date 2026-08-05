package com.ekko.seller_service.controller;

import com.ekko.seller_service.dto.response.SellerMetricsResponse;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.support.SellerResolver;
import com.ekko.seller_service.service.SellerMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sellers/metrics")
@PreAuthorize("hasRole('SELLER')")
public class SellerMetricsController {

    private final SellerMetricsService sellerMetricsService;
    private final SellerResolver sellerResolver;

    @GetMapping("/me")
    public ResponseEntity<SellerMetricsResponse> getMyMetrics(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Seller seller = sellerResolver.resolve(jwt.getSubject());

        return ResponseEntity.ok(
                sellerMetricsService.getMyMetrics(seller.getId())
        );
    }
}