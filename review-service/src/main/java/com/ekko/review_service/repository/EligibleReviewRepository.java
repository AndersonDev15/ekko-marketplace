package com.ekko.review_service.repository;

import com.ekko.review_service.entity.EligibleReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EligibleReviewRepository extends JpaRepository<EligibleReview, UUID> {

    Optional<EligibleReview> findByOrderItemIdAndCustomerId(UUID orderItemId, String customerId);

    @Query("""
            SELECT er FROM EligibleReview er
            WHERE er.customerId = :customerId
              AND NOT EXISTS (
                  SELECT 1 FROM Review r
                  WHERE r.orderItemId = er.orderItemId AND r.customerId = er.customerId
              )
            """)
    List<EligibleReview> findEligibleWithoutReview(@Param("customerId") String customerId);

    @Modifying
    @Query(value = """
            INSERT INTO eligible_reviews (order_id, order_item_id, product_id, customer_id)
            VALUES (:orderId, :orderItemId, :productId, :customerId)
            ON CONFLICT (order_item_id, customer_id) DO NOTHING
            """, nativeQuery = true)
    void insertEligibilityIfAbsent(
            @Param("orderId") UUID orderId,
            @Param("orderItemId") UUID orderItemId,
            @Param("productId") UUID productId,
            @Param("customerId") String customerId);
}