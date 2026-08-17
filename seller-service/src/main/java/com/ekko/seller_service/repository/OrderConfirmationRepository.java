package com.ekko.seller_service.repository;

import com.ekko.seller_service.entity.OrderConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface OrderConfirmationRepository extends JpaRepository<OrderConfirmation, UUID> {

    /**
     * Marks the order as processed for metrics, atomically and idempotently.
     * Returns 1 when the order was not processed before, 0 when it was already
     * marked (a redelivery of order.confirmed that must not double-count metrics).
     */
    @Modifying
    @Query(value = """
            INSERT INTO order_confirmations (id, order_id, created_at)
            VALUES (gen_random_uuid(), :orderId, now())
            ON CONFLICT (order_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("orderId") UUID orderId);
}