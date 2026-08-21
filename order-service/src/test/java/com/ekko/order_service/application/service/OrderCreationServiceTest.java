package com.ekko.order_service.application.service;

import com.ekko.order_service.domain.exception.InsufficientStockException;
import com.ekko.order_service.domain.exception.InvalidGuestEmailException;
import com.ekko.order_service.application.exception.OrderNumberGenerationException;
import com.ekko.order_service.application.exception.ProductVariantNotFoundException;
import com.ekko.order_service.application.exception.StockReservationException;
import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.model.OrderAddress;
import com.ekko.order_service.domain.event.OrderCreatedEvent;
import com.ekko.order_service.domain.model.OrderDraft;
import com.ekko.order_service.domain.enums.OrderStatus;
import com.ekko.order_service.domain.model.StockItem;
import com.ekko.order_service.domain.port.out.OrderEventPublisherPort;
import com.ekko.order_service.domain.port.out.OrderRepositoryPort;
import com.ekko.order_service.domain.port.out.ProductServicePort;
import com.ekko.order_service.domain.service.OrderNumberGenerator;
import com.ekko.order_service.domain.service.OrderTotalsCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.ekko.order_service.builder.OrderTestDataBuilder.anOrder;
import static com.ekko.order_service.builder.OrderTestDataBuilder.anOrderAddress;
import static com.ekko.order_service.builder.OrderTestDataBuilder.aProductVariant;
import static com.ekko.order_service.util.TestConstants.CUSTOMER_KEYCLOAK_ID;
import static com.ekko.order_service.util.TestConstants.VARIANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCreationServiceTest {

    @Mock
    private ProductServicePort productServicePort;
    @Mock
    private OrderRepositoryPort orderRepositoryPort;
    @Mock
    private OrderTransactionService orderTransactionService;
    @Mock
    private OrderEventPublisherPort orderEventPublisherPort;
    @Mock
    private OrderNumberGenerator orderNumberGenerator;

    private final OrderTotalsCalculator orderTotalsCalculator = new OrderTotalsCalculator();

    private OrderCreationService service;

    @BeforeEach
    void setUp() {
        service = new OrderCreationService(
                productServicePort,
                orderRepositoryPort,
                orderTransactionService,
                orderEventPublisherPort,
                orderNumberGenerator,
                orderTotalsCalculator);
    }

    private OrderDraft validDraft() {
        return new OrderDraft(
                CUSTOMER_KEYCLOAK_ID,
                null,
                "customer@example.com",
                anOrderAddress(),
                List.of(new OrderDraft.OrderItemDraft(VARIANT_ID, 2)),
                null);
    }

    private void stubHappyPath() {
        when(orderNumberGenerator.generate()).thenReturn("EKK-20250809-AB12");
        when(orderRepositoryPort.findByOrderNumber("EKK-20250809-AB12"))
                .thenReturn(Optional.empty());
        when(productServicePort.getVariantsInfo(anyList()))
                .thenReturn(List.of(aProductVariant(10L)));
    }

    @Test
    void createsOrderPublishingEventAfterPersisting() {
        stubHappyPath();

        Order savedMock = anOrder();
        when(orderTransactionService.commitOrder(any(Order.class))).thenReturn(savedMock);

        Order result = service.execute(validDraft());

        assertNotNull(result);
        InOrder inOrder = inOrder(productServicePort, orderTransactionService, orderEventPublisherPort);
        inOrder.verify(productServicePort).reserveStock(anyList());
        inOrder.verify(orderTransactionService).commitOrder(any(Order.class));
        inOrder.verify(orderEventPublisherPort).publishOrderCreated(any(OrderCreatedEvent.class));
    }

    @Test
    void reservesExactlyOneStockItemPerFlexibleLine() {
        stubHappyPath();
        when(orderTransactionService.commitOrder(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(validDraft());

        verify(productServicePort).reserveStock(
                List.of(new StockItem(VARIANT_ID, 2)));
    }

    @Test
    void commitsBuiltOrderWithPendingStatusAndTotals() {
        stubHappyPath();
        when(orderTransactionService.commitOrder(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order result = service.execute(validDraft());

        verify(orderTransactionService).commitOrder(argThat(order ->
                order.getStatus() == OrderStatus.PENDING
                        && new BigDecimal("200.00").compareTo(order.getTotal()) == 0
                        && new BigDecimal("200.00").compareTo(order.getSubtotal()) == 0
                        && order.getCustomerId().equals(CUSTOMER_KEYCLOAK_ID)
                        && order.getItems().size() == 1
                        && order.getItems().get(0).variantId().equals(VARIANT_ID)));
        verify(orderEventPublisherPort).publishOrderCreated(any(OrderCreatedEvent.class));
    }

    @Test
    void publishesEventWithSavedOrderFields() {
        stubHappyPath();
        Order savedMock = anOrder();
        when(orderTransactionService.commitOrder(any(Order.class))).thenReturn(savedMock);

        service.execute(validDraft());

        verify(orderEventPublisherPort).publishOrderCreated(argThat(event ->
                savedMock.getOrderNumber().equals(event.orderNumber())
                        && savedMock.getStatus() == event.status()
                        && savedMock.getTotal().equals(event.total())));
    }

    @Test
    void publishesCustomerIdAndCustomerEmailForAuthenticatedCustomer() {
        stubHappyPath();
        when(orderTransactionService.commitOrder(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(new OrderDraft(
                CUSTOMER_KEYCLOAK_ID,
                null,
                "customer@example.com",
                anOrderAddress(),
                List.of(new OrderDraft.OrderItemDraft(VARIANT_ID, 2)),
                null));

        verify(orderEventPublisherPort).publishOrderCreated(argThat(event ->
                CUSTOMER_KEYCLOAK_ID.equals(event.customerId())
                        && "customer@example.com".equals(event.customerEmail())));
    }

    @Test
    void publishesNullCustomerIdWithGuestEmailForGuestCheckout() {
        when(orderNumberGenerator.generate()).thenReturn("EKK-20250809-AB12");
        when(orderRepositoryPort.findByOrderNumber("EKK-20250809-AB12"))
                .thenReturn(Optional.empty());
        when(productServicePort.getVariantsInfo(anyList()))
                .thenReturn(List.of(aProductVariant(10L)));
        when(orderTransactionService.commitOrder(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(new OrderDraft(
                null,
                "guest@example.com",
                "guest@example.com",
                anOrderAddress(),
                List.of(new OrderDraft.OrderItemDraft(VARIANT_ID, 1)),
                null));

        verify(orderEventPublisherPort).publishOrderCreated(argThat(event ->
                event.customerId() == null
                        && "guest@example.com".equals(event.customerEmail())));
    }

    @Test
    void rejectsDraftWithoutCustomerIdOrGuestEmail() {
        OrderDraft draft = new OrderDraft(
                null, null, null, anOrderAddress(),
                List.of(new OrderDraft.OrderItemDraft(VARIANT_ID, 1)), null);

        assertThrows(InvalidGuestEmailException.class, () -> service.execute(draft));
        verifyNoInteractions(productServicePort, orderRepositoryPort,
                orderTransactionService, orderEventPublisherPort);
    }

    @Test
    void rejectsBlankGuestEmail() {
        OrderDraft draft = new OrderDraft(
                null, "   ", "   ", anOrderAddress(),
                List.of(new OrderDraft.OrderItemDraft(VARIANT_ID, 1)), null);

        assertThrows(InvalidGuestEmailException.class, () -> service.execute(draft));
        verifyNoInteractions(productServicePort, orderRepositoryPort,
                orderTransactionService, orderEventPublisherPort);
    }

    @Test
    void rejectsEmptyItemsList() {
        OrderDraft draft = new OrderDraft(
                CUSTOMER_KEYCLOAK_ID, null, "customer@example.com", anOrderAddress(), List.of(), null);

        assertThrows(ProductVariantNotFoundException.class, () -> service.execute(draft));
        verifyNoInteractions(productServicePort, orderRepositoryPort,
                orderTransactionService, orderEventPublisherPort);
    }

    @Test
    void propagatesStockReservationExceptionWithoutSaving() {
        when(productServicePort.getVariantsInfo(anyList()))
                .thenThrow(new StockReservationException("product-service unavailable"));

        assertThrows(StockReservationException.class, () -> service.execute(validDraft()));

        verify(orderTransactionService, never()).commitOrder(any(Order.class));
        verify(orderEventPublisherPort, never()).publishOrderCreated(any(OrderCreatedEvent.class));
    }

    @Test
    void throwsProductVariantNotFoundWhenVariantMissingFromResponse() {
        when(productServicePort.getVariantsInfo(anyList())).thenReturn(List.of());

        assertThrows(ProductVariantNotFoundException.class,
                () -> service.execute(validDraft()));

        verify(orderTransactionService, never()).commitOrder(any(Order.class));
        verify(orderEventPublisherPort, never()).publishOrderCreated(any(OrderCreatedEvent.class));
    }

    @Test
    void throwsInsufficientStockBeforeReservingWhenLocalStockIsLow() {
        when(productServicePort.getVariantsInfo(anyList()))
                .thenReturn(List.of(aProductVariant(1L)));

        InsufficientStockException exception = assertThrows(InsufficientStockException.class,
                () -> service.execute(validDraft()));

        assertEquals(VARIANT_ID, exception.getVariantId());
        assertEquals(2L, exception.getRequestedQuantity());
        verify(productServicePort, never()).reserveStock(anyList());
        verify(orderTransactionService, never()).commitOrder(any(Order.class));
        verify(orderEventPublisherPort, never()).publishOrderCreated(any(OrderCreatedEvent.class));
    }

    @Test
    void propagatesInsufficientStockFromReservationWithoutCommitting() {
        when(orderNumberGenerator.generate()).thenReturn("EKK-20250809-AB12");
        when(orderRepositoryPort.findByOrderNumber("EKK-20250809-AB12"))
                .thenReturn(Optional.empty());
        when(productServicePort.getVariantsInfo(anyList()))
                .thenReturn(List.of(aProductVariant(100L)));
        doThrow(new InsufficientStockException("race: stock agotado"))
                .when(productServicePort).reserveStock(anyList());

        assertThrows(InsufficientStockException.class, () -> service.execute(validDraft()));

        verify(orderTransactionService, never()).commitOrder(any(Order.class));
        verify(orderEventPublisherPort, never()).publishOrderCreated(any(OrderCreatedEvent.class));
    }

    @Test
    void retriesOrderNumberWhenCandidateIsTaken() {
        when(productServicePort.getVariantsInfo(anyList()))
                .thenReturn(List.of(aProductVariant(10L)));
        when(orderNumberGenerator.generate())
                .thenReturn("EKK-20250809-ABCD", "EKK-20250809-EFGH");
        when(orderRepositoryPort.findByOrderNumber("EKK-20250809-ABCD"))
                .thenReturn(Optional.of(anOrder()));
        when(orderRepositoryPort.findByOrderNumber("EKK-20250809-EFGH"))
                .thenReturn(Optional.empty());
        when(orderTransactionService.commitOrder(any(Order.class)))
                .thenReturn(anOrder());

        service.execute(validDraft());

        verify(orderRepositoryPort).findByOrderNumber("EKK-20250809-ABCD");
        verify(orderRepositoryPort).findByOrderNumber("EKK-20250809-EFGH");
        verify(orderTransactionService).commitOrder(
                argThat(order -> order.getOrderNumber().equals("EKK-20250809-EFGH")));
    }

    @Test
    void givesUpAfterThreeTakenCandidates() {
        when(productServicePort.getVariantsInfo(anyList()))
                .thenReturn(List.of(aProductVariant(10L)));
        when(orderNumberGenerator.generate())
                .thenReturn("EKK-20250809-AAA1");
        when(orderRepositoryPort.findByOrderNumber(anyString()))
                .thenReturn(Optional.of(anOrder()));

        assertThrows(OrderNumberGenerationException.class, () -> service.execute(validDraft()));

        verify(orderRepositoryPort, org.mockito.Mockito.times(3))
                .findByOrderNumber(anyString());
        verify(orderTransactionService, never()).commitOrder(any(Order.class));
        verify(orderEventPublisherPort, never()).publishOrderCreated(any(OrderCreatedEvent.class));
    }

    @Test
    void doesNotPublishEventWhenPersistenceFails() {
        when(orderNumberGenerator.generate()).thenReturn("EKK-20250809-AB12");
        when(orderRepositoryPort.findByOrderNumber("EKK-20250809-AB12"))
                .thenReturn(Optional.empty());
        when(productServicePort.getVariantsInfo(anyList()))
                .thenReturn(List.of(aProductVariant(10L)));
        when(orderTransactionService.commitOrder(any(Order.class)))
                .thenThrow(new RuntimeException("db failure"));

        assertThrows(RuntimeException.class, () -> service.execute(validDraft()));

        verify(orderEventPublisherPort, never()).publishOrderCreated(any(OrderCreatedEvent.class));
    }

    @Test
    void storesGuestEmailForGuestCheckout() {
        OrderAddress address = anOrderAddress();
        when(orderNumberGenerator.generate()).thenReturn("EKK-20250809-AB12");
        when(orderRepositoryPort.findByOrderNumber("EKK-20250809-AB12"))
                .thenReturn(Optional.empty());
        when(productServicePort.getVariantsInfo(anyList()))
                .thenReturn(List.of(aProductVariant(10L)));
        when(orderTransactionService.commitOrder(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderDraft draft = new OrderDraft(
                null,
                " guest@example.com ",
                "guest@example.com",
                address,
                List.of(new OrderDraft.OrderItemDraft(VARIANT_ID, 1)),
                null);

        service.execute(draft);

        verify(orderTransactionService).commitOrder(
                argThat(order ->
                        order.getCustomerEmail().equals("guest@example.com")
                                && order.getCustomerId() == null
                                && order.getStatus() == OrderStatus.PENDING));
    }
}