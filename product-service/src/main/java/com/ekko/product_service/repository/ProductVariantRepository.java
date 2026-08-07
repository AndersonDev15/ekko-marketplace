package com.ekko.product_service.repository;

import com.ekko.product_service.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, UUID id);

    long countByProductIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot(UUID productId, UUID id);
}