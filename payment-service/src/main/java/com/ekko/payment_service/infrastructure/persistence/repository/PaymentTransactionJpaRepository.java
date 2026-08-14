package com.ekko.payment_service.infrastructure.persistence.repository;

import com.ekko.payment_service.infrastructure.persistence.entity.PaymentTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentTransactionJpaRepository extends JpaRepository<PaymentTransactionEntity, UUID> {

    boolean existsByStripeEventId(String stripeEventId);

    List<PaymentTransactionEntity> findByPaymentId(UUID paymentId);
}