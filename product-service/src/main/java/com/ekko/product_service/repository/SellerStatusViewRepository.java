package com.ekko.product_service.repository;

import com.ekko.product_service.entity.SellerStatusView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SellerStatusViewRepository extends JpaRepository<SellerStatusView, UUID> {
}
