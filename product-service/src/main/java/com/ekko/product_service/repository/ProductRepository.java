package com.ekko.product_service.repository;

import com.ekko.product_service.entity.Product;
import com.ekko.product_service.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    boolean existsBySellerKeycloakIdAndSlug(UUID sellerKeycloakId, String slug);

    Page<Product> findBySellerKeycloakIdAndDeletedAtIsNull(UUID sellerKeycloakId, Pageable pageable);

    Optional<Product> findBySellerSlugAndSlugAndStatusAndDeletedAtIsNull(
            String sellerSlug,
            String productSlug,
            ProductStatus status);

    Page<Product> findByStatusAndDeletedAtIsNull(ProductStatus status, Pageable pageable);
}