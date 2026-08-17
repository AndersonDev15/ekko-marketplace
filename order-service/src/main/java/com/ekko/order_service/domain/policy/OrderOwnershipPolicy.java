package com.ekko.order_service.domain.policy;

import com.ekko.order_service.application.exception.OrderAccessDeniedException;
import com.ekko.order_service.domain.model.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderOwnershipPolicy {

    public void assertCustomerOwns(Order order, UUID keycloakId) {
        if (order.getCustomerId() == null ||
                !order.getCustomerId().equals(keycloakId)) {
            throw new OrderAccessDeniedException();
        }
    }

    public void assertGuestOwns(Order order, String guestEmail) {
        if (order.getGuestEmail() == null ||
                !order.getGuestEmail().equals(guestEmail)) {
            throw new OrderAccessDeniedException();
        }
    }
}