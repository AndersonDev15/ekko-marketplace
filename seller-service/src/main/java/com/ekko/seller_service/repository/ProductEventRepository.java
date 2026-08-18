package com.ekko.seller_service.repository;

import com.ekko.seller_service.entity.ProductEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProductEventRepository extends JpaRepository<ProductEvent, UUID> {

    /**
     * Marks the product event as processed for metrics, atomically and idempotently.
     * The same product can be published and later deactivated, so the key is the
     * pair (productId, eventType). Returns 1 when it was not processed before,
     * 0 when it was already marked (a redelivery that must not count twice).
     */
    @Modifying
    @Query(value = """
            INSERT INTO product_events (id, product_id, event_type, created_at)
            VALUES (gen_random_uuid(), :productId, :eventType, now())
            ON CONFLICT (product_id, event_type) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("productId") UUID productId, @Param("eventType") String eventType);
}