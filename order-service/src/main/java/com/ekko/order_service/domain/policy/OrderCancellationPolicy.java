package com.ekko.order_service.domain.policy;

import com.ekko.order_service.domain.exception.OrderCancellationNotAllowedException;
import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.model.OrderStatus;
import org.springframework.stereotype.Component;

@Component
public class OrderCancellationPolicy {

    public void assertCancellable(Order order) {
        OrderStatus status = order.getStatus();
        if (status != OrderStatus.PENDING && status != OrderStatus.CONFIRMED) {
            throw new OrderCancellationNotAllowedException(status);
        }
    }
}