package com.ekko.product_service.repository;

import com.ekko.product_service.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, UUID id);

    long countByProductIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot(UUID productId, UUID id);

    @Query("SELECT v FROM ProductVariant v JOIN FETCH v.product p WHERE v.id IN :variantIds")
    List<ProductVariant> findAllWithProductByIds(@Param("variantIds") List<UUID> variantIds);
}