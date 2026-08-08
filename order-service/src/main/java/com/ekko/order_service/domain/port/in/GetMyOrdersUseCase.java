package com.ekko.order_service.domain.port.in;

import com.ekko.order_service.domain.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface GetMyOrdersUseCase {

    Page<Order> execute(UUID keycloakId, Pageable pageable);
}