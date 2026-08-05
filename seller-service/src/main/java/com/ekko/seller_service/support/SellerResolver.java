package com.ekko.seller_service.support;

import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.exception.SellerNotFoundException;
import com.ekko.seller_service.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SellerResolver {

    private final SellerRepository sellerRepository;

    public Seller resolve(String keycloakId) {
        return sellerRepository.findByKeycloakId(keycloakId)
                .orElseThrow(()-> new SellerNotFoundException(keycloakId));
    }
}
