package com.ekko.product_service.repository;

import com.ekko.product_service.entity.ProductVariantAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductVariantAttributeRepository extends JpaRepository<ProductVariantAttribute, UUID> {
}