package com.ekko.product_service.service;

import com.ekko.product_service.messaging.dto.consume.OrderCancelledEvent;
import com.ekko.product_service.messaging.dto.consume.OrderConfirmedEvent;
import com.ekko.product_service.repository.OrderEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderInventoryServiceTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID VARIANT_1 = UUID.randomUUID();
    private static final UUID VARIANT_2 = UUID.randomUUID();

    @Mock
    private OrderEventRepository orderEventRepository;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderInventoryService orderInventoryService;

    @Test
    void confirmStock_llamaConfirmStockPorCadaItem() {
        when(orderEventRepository.insertIfAbsent(ORDER_ID, OrderInventoryService.EVENT_CONFIRMED)).thenReturn(1);

        orderInventoryService.confirmStock(confirmedEvent(2));

        verify(inventoryService).confirmStock(VARIANT_1, 2);
        verify(inventoryService).confirmStock(VARIANT_2, 1);
    }

    @Test
    void confirmStock_ordenYaProcesada_noAjustaStock() {
        when(orderEventRepository.insertIfAbsent(ORDER_ID, OrderInventoryService.EVENT_CONFIRMED)).thenReturn(0);

        orderInventoryService.confirmStock(confirmedEvent(2));

        verifyNoInteractions(inventoryService);
    }

    @Test
    void releaseStock_llamaReleaseStockPorCadaItem() {
        when(orderEventRepository.insertIfAbsent(ORDER_ID, OrderInventoryService.EVENT_CANCELLED)).thenReturn(1);

        orderInventoryService.releaseStock(cancelledEvent(2));

        verify(inventoryService).releaseStock(VARIANT_1, 2);
        verify(inventoryService).releaseStock(VARIANT_2, 1);
    }

    @Test
    void releaseStock_ordenYaProcesada_noAjustaStock() {
        when(orderEventRepository.insertIfAbsent(ORDER_ID, OrderInventoryService.EVENT_CANCELLED)).thenReturn(0);

        orderInventoryService.releaseStock(cancelledEvent(1));

        verifyNoInteractions(inventoryService);
    }

    @Test
    void confirmacionYCancelacionMismaOrden_sonIndependientes() {
        when(orderEventRepository.insertIfAbsent(ORDER_ID, OrderInventoryService.EVENT_CONFIRMED)).thenReturn(1);
        when(orderEventRepository.insertIfAbsent(ORDER_ID, OrderInventoryService.EVENT_CANCELLED)).thenReturn(1);

        assertDoesNotThrow(() -> {
            orderInventoryService.confirmStock(confirmedEvent(1));
            orderInventoryService.releaseStock(cancelledEvent(1));
        });
    }

    private static OrderConfirmedEvent confirmedEvent(int quantity) {
        return new OrderConfirmedEvent(
                ORDER_ID, "EKK-001", UUID.randomUUID(), "guest@ekko.test",
                List.of(
                        new OrderConfirmedEvent.OrderItemConfirmed(
                                UUID.randomUUID(), UUID.randomUUID(), VARIANT_1, quantity,
                                UUID.randomUUID(), new BigDecimal("100.00")),
                        new OrderConfirmedEvent.OrderItemConfirmed(
                                UUID.randomUUID(), UUID.randomUUID(), VARIANT_2, 1,
                                UUID.randomUUID(), new BigDecimal("50.00"))),
                null);
    }

    private static OrderCancelledEvent cancelledEvent(int quantity) {
        return new OrderCancelledEvent(
                ORDER_ID, "EKK-001", UUID.randomUUID(), null, "CONFIRMED", true,
                List.of(
                        new OrderCancelledEvent.OrderItemCancelled(
                                UUID.randomUUID(), UUID.randomUUID(), VARIANT_1, quantity),
                        new OrderCancelledEvent.OrderItemCancelled(
                                UUID.randomUUID(), UUID.randomUUID(), VARIANT_2, 1)),
                null);
    }
}