package com.ekko.order_service.domain.port.in;

import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.model.OrderDraft;

public interface CreateOrderUseCase {

    Order execute(OrderDraft draft);
}