package com.ekko.payment_service.infrastructure.persistence.repository;

import com.ekko.payment_service.domain.model.RefundStatus;
import com.ekko.payment_service.infrastructure.persistence.entity.RefundEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface RefundJpaRepository extends JpaRepository<RefundEntity, UUID> {

    Optional<RefundEntity> findByStripeRefundId(String stripeRefundId);

    @Query("""
            SELECT SUM(r.amount)
            FROM RefundEntity r
            WHERE r.payment.id = :paymentId
              AND r.status = :status
            """)
    BigDecimal sumAmountByPaymentIdAndStatus(@Param("paymentId") UUID paymentId,
                                             @Param("status") RefundStatus status);
}