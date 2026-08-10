package com.ekko.order_service.application.service;

import com.ekko.order_service.domain.exception.OrderAccessDeniedException;
import com.ekko.order_service.domain.exception.OrderNotFoundException;
import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.policy.OrderOwnershipPolicy;
import com.ekko.order_service.domain.port.out.OrderRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static com.ekko.order_service.builder.OrderTestDataBuilder.aGuestOrder;
import static com.ekko.order_service.builder.OrderTestDataBuilder.anOrder;
import static com.ekko.order_service.util.TestConstants.CUSTOMER_KEYCLOAK_ID;
import static com.ekko.order_service.util.TestConstants.GUEST_EMAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    @Mock
    private OrderRepositoryPort orderRepositoryPort;

    private OrderQueryService service;

    private OrderQueryService createService() {
        return new OrderQueryService(orderRepositoryPort, new OrderOwnershipPolicy());
    }

    @Test
    void returnsOrderForCustomerWhenOwned() {
        service = createService();
        Order order = anOrder();
        when(orderRepositoryPort.findByOrderNumber(order.getOrderNumber()))
                .thenReturn(Optional.of(order));

        Order result = service.execute(order.getOrderNumber(), CUSTOMER_KEYCLOAK_ID, null);

        assertEquals(order, result);
    }

    @Test
    void returnsOrderForGuestWhenOwned() {
        service = createService();
        Order order = aGuestOrder();
        when(orderRepositoryPort.findByOrderNumber(order.getOrderNumber()))
                .thenReturn(Optional.of(order));

        Order result = service.execute(order.getOrderNumber(), null, GUEST_EMAIL);

        assertEquals(order, result);
    }

    @Test
    void throwsOrderNotFoundExceptionWhenMissing() {
        service = createService();
        when(orderRepositoryPort.findByOrderNumber("EKK-00000000-XXXX"))
                .thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> service.execute("EKK-00000000-XXXX", CUSTOMER_KEYCLOAK_ID, null));
    }

    @Test
    void throwsOrderAccessDeniedWhenCustomerDoesNotOwnOrder() {
        service = createService();
        Order order = anOrder();
        when(orderRepositoryPort.findByOrderNumber(order.getOrderNumber()))
                .thenReturn(Optional.of(order));

        assertThrows(OrderAccessDeniedException.class,
                () -> service.execute(order.getOrderNumber(), java.util.UUID.randomUUID(), null));
    }

    @Test
    void throwsOrderAccessDeniedWhenGuestEmailDoesNotMatch() {
        service = createService();
        Order order = aGuestOrder();
        when(orderRepositoryPort.findByOrderNumber(order.getOrderNumber()))
                .thenReturn(Optional.of(order));

        assertThrows(OrderAccessDeniedException.class,
                () -> service.execute(order.getOrderNumber(), null, "other@example.com"));
    }

    @Test
    void throwsOrderAccessDeniedWhenOrderHasNoOwnerIdentifier() {
        service = createService();
        when(orderRepositoryPort.findByOrderNumber("EKK-LONELY-0000"))
                .thenReturn(Optional.of(new Order()));

        assertThrows(OrderAccessDeniedException.class,
                () -> service.execute("EKK-LONELY-0000", CUSTOMER_KEYCLOAK_ID, null));
    }

    @Test
    void returnsMyOrdersByCustomerId() {
        service = createService();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(anOrder()));
        when(orderRepositoryPort.findAllByCustomerId(CUSTOMER_KEYCLOAK_ID, pageable))
                .thenReturn(page);

        Page<Order> result = service.execute(CUSTOMER_KEYCLOAK_ID, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void returnsAllOrdersForAdmin() {
        service = createService();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(anOrder(), aGuestOrder()));
        when(orderRepositoryPort.findAll(pageable)).thenReturn(page);

        Page<Order> result = service.execute(pageable);

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void returnsOrderForAdminWithoutOwnershipCheck() {
        service = createService();
        Order order = anOrder();
        when(orderRepositoryPort.findByOrderNumber(order.getOrderNumber()))
                .thenReturn(Optional.of(order));

        Order result = service.execute(order.getOrderNumber());

        assertEquals(order, result);
    }
}