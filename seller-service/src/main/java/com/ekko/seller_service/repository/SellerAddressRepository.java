package com.ekko.seller_service.repository;

import com.ekko.seller_service.entity.SellerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SellerAddressRepository extends JpaRepository<SellerAddress, UUID> {
    List<SellerAddress> findBySellerId(UUID sellerId);

    Optional<SellerAddress> findByIdAndSellerId(UUID id, UUID sellerId);
    boolean existsBySellerId(UUID sellerId);

    long countBySellerId(UUID sellerId);
    @Modifying(clearAutomatically = true)
    @Query("""
    UPDATE SellerAddress a
    SET a.primary = false
    WHERE a.seller.id = :sellerId
""")
    void clearPrimaryBySellerId(UUID sellerId);
}
