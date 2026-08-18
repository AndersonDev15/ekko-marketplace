package com.ekko.product_service.repository;

import com.ekko.product_service.entity.ReviewEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ReviewEventRepository extends JpaRepository<ReviewEvent, UUID> {

    /**
     * Marks the review as processed for rating updates, atomically and idempotently.
     * Returns 1 when the review was not processed before, 0 when it was already
     * marked (a redelivery of review.created that must not count twice).
     */
    @Modifying
    @Query(value = """
            INSERT INTO review_events (id, review_id, created_at)
            VALUES (gen_random_uuid(), :reviewId, now())
            ON CONFLICT (review_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("reviewId") UUID reviewId);
}