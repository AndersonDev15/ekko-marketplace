package com.ekko.seller_service.repository;

import com.ekko.seller_service.entity.ReviewConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ReviewConfirmationRepository extends JpaRepository<ReviewConfirmation, UUID> {

    /**
     * Marks the review as processed for metrics, atomically and idempotently.
     * Returns 1 when the review was not processed before, 0 when it was already
     * marked (a redelivery of review.created that must not double-count metrics).
     */
    @Modifying
    @Query(value = """
            INSERT INTO review_confirmations (id, review_id, created_at)
            VALUES (gen_random_uuid(), :reviewId, now())
            ON CONFLICT (review_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("reviewId") UUID reviewId);
}