package com.ekko.order_service.infrastructure.persistence.repository;

import com.ekko.order_service.infrastructure.persistence.entity.OrderPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderPaymentJpaRepository extends JpaRepository<OrderPaymentEntity, UUID> {
}