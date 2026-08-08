package com.ekko.order_service.domain.port.in;

import com.ekko.order_service.domain.model.Order;

public interface GetOrderByOrderNumberForAdminUseCase {

    Order execute(String orderNumber);
}