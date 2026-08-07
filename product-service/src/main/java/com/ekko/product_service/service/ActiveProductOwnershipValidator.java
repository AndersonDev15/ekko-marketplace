package com.ekko.product_service.service;

import com.ekko.product_service.entity.Product;
import com.ekko.product_service.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ActiveProductOwnershipValidator {

    private final OwnershipValidator ownershipValidator;

    public Product validate(UUID productId, UUID sellerKeycloakId) {
        Product product = ownershipValidator.validate(productId, sellerKeycloakId);

        if (product.getDeletedAt() != null) {
            throw new ProductNotFoundException();
        }

        return product;
    }
}
