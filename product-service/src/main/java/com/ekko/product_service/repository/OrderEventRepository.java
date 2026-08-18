package com.ekko.product_service.repository;

import com.ekko.product_service.entity.OrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface OrderEventRepository extends JpaRepository<OrderEvent, UUID> {

    /**
     * Marks the order event as processed for inventory, atomically and idempotently.
     * The same order can be confirmed and later cancelled, so the key is the pair
     * (orderId, eventType). Returns 1 when it was not processed before, 0 when it
     * was already marked (a redelivery that must not adjust stock twice).
     */
    @Modifying
    @Query(value = """
            INSERT INTO order_events (id, order_id, event_type, created_at)
            VALUES (gen_random_uuid(), :orderId, :eventType, now())
            ON CONFLICT (order_id, event_type) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("orderId") UUID orderId, @Param("eventType") String eventType);
}