package com.ekko.order_service.domain.port.in;

import com.ekko.order_service.domain.model.Order;

import java.util.UUID;

public interface CancelOrderUseCase {

    Order execute(String orderNumber, UUID keycloakId, String guestEmail, boolean isAdmin);
}