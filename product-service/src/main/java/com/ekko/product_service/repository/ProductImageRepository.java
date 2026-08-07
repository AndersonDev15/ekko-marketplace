package com.ekko.product_service.repository;

import com.ekko.product_service.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    List<ProductImage> findByProductIdOrderBySortOrderAsc(UUID productId);

    Optional<ProductImage> findByProductIdAndIsPrimaryTrue(UUID productId);
}