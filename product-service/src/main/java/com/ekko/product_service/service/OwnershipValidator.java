package com.ekko.product_service.service;

import com.ekko.product_service.entity.Product;
import com.ekko.product_service.exception.ForbiddenProductAccessException;
import com.ekko.product_service.exception.ProductNotFoundException;
import com.ekko.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OwnershipValidator {

    private final ProductRepository productRepository;

    public Product validate(UUID productId, UUID sellerKeycloakId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        if (!product.getSellerKeycloakId().equals(sellerKeycloakId)) {
            throw new ForbiddenProductAccessException();
        }

        return product;
    }
}