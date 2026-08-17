package com.ekko.payment_service.infrastructure.persistence.repository;

import com.ekko.payment_service.domain.enums.PaymentStatus;
import com.ekko.payment_service.infrastructure.persistence.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByOrderIdAndStatus(UUID orderId, PaymentStatus status);

    Optional<PaymentEntity> findByOrderId(UUID orderId);

    Optional<PaymentEntity> findByPaymentIntentId(String paymentIntentId);

    Optional<PaymentEntity> findFirstByCustomerIdAndStripeCustomerIdIsNotNullOrderByCreatedAtDesc(UUID customerId);
}
