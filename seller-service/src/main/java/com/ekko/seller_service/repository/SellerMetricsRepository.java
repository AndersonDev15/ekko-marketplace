package com.ekko.seller_service.repository;

import com.ekko.seller_service.entity.SellerMetrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SellerMetricsRepository extends JpaRepository<SellerMetrics, Long> {
    Optional<SellerMetrics> findBySellerId(UUID sellerId);

}
