package com.ekko.order_service.infrastructure.persistence.repository;

import com.ekko.order_service.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    Page<OrderEntity> findAllByCustomerId(UUID customerId, Pageable pageable);

    boolean existsByOrderNumber(String orderNumber);
}