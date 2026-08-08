package com.ekko.order_service.infrastructure.persistence.repository;

import com.ekko.order_service.infrastructure.persistence.entity.OrderStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderStatusHistoryJpaRepository extends JpaRepository<OrderStatusHistoryEntity, UUID> {
}