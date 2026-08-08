package com.ekko.order_service.domain.port.out;

import com.ekko.order_service.domain.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepositoryPort {

    Order save(Order order);

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findAllByCustomerId(UUID customerId, Pageable pageable);

    Page<Order> findAll(Pageable pageable);
}