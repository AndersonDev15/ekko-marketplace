package com.ekko.product_service.repository;

import com.ekko.product_service.entity.SellerSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SellerSnapshotRepository extends JpaRepository<SellerSnapshot, UUID> {

    List<SellerSnapshot> findAllBySellerKeycloakIdIn(Collection<UUID> sellerKeycloakIds);
}
