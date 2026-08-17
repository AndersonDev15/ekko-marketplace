package com.ekko.order_service.domain.port.in;

import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.enums.OrderStatus;

public interface UpdateOrderStatusUseCase {

    Order execute(String orderNumber, OrderStatus newStatus);
}