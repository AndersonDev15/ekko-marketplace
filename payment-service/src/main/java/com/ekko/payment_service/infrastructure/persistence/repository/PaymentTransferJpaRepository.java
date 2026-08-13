package com.ekko.payment_service.infrastructure.persistence.repository;

import com.ekko.payment_service.infrastructure.persistence.entity.PaymentTransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentTransferJpaRepository extends JpaRepository<PaymentTransferEntity, UUID> {

    List<PaymentTransferEntity> findByPaymentId(UUID paymentId);
}