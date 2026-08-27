package com.ekko.seller_service.repository;

import com.ekko.seller_service.dto.response.SellerSummaryResponse;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.enums.SellerStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface SellerRepository extends JpaRepository<Seller, UUID> {

    Optional<Seller> findByKeycloakId(String keycloakId);
    boolean existsBySlug(String slug);


    @Query("""
        SELECT s FROM Seller s
        WHERE s.status = COALESCE(:status, s.status)
          AND s.createdAt >= COALESCE(:from, s.createdAt)
          AND s.createdAt <= COALESCE(:to, s.createdAt)
        """)
    Page<Seller> findAllFiltered(@Param("status") SellerStatus status,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to,
                                 Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seller s WHERE s.id = :sellerId")
    Optional<Seller> findByIdForUpdate(UUID sellerId);
}
