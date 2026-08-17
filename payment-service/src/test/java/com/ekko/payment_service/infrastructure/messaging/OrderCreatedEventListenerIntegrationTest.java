package com.ekko.payment_service.infrastructure.messaging;

import com.ekko.payment_service.config.AbstractRabbitMqIntegrationTest;
import com.ekko.payment_service.domain.command.InitiatePaymentCommand;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.port.in.InitiatePaymentUseCase;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.infrastructure.config.RabbitMQConfig;
import com.ekko.payment_service.infrastructure.messaging.dto.OrderCreatedEventPayload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderCreatedEventListenerIntegrationTest extends AbstractRabbitMqIntegrationTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID SELLER_ONE = UUID.randomUUID();
    private static final UUID SELLER_TWO = UUID.randomUUID();
    private static final UUID VARIANT_ONE = UUID.randomUUID();
    private static final UUID VARIANT_TWO = UUID.randomUUID();
    private static final UUID VARIANT_THREE = UUID.randomUUID();

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private InitiatePaymentUseCase initiatePaymentUseCase;

    @MockitoBean
    private PaymentRepositoryPort paymentRepositoryPort;

    @AfterEach
    void resetMocks() {
        org.mockito.Mockito.reset(paymentRepositoryPort);
    }

    @Test
    void receivesGuestOrderCreatedAndBuildsInitiatePaymentCommand() {
        when(paymentRepositoryPort.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                guestPayload());

        ArgumentCaptor<InitiatePaymentCommand> captor = ArgumentCaptor.forClass(InitiatePaymentCommand.class);
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(initiatePaymentUseCase).execute(captor.capture()));

        InitiatePaymentCommand command = captor.getValue();
        assertEquals(ORDER_ID, command.orderId());
        assertNull(command.customerId());
        assertEquals("guest@example.com", command.guestEmail());
        assertEquals("guest@example.com", command.customerEmail());
        assertEquals(0, new BigDecimal("250.00").compareTo(command.amount()));
        assertEquals("USD", command.currency());

        assertEquals(2, command.vendorGrossAmounts().size());
        InitiatePaymentCommand.VendorGrossAmount sellerOne = command.vendorGrossAmounts().stream()
                .filter(v -> v.vendorId().equals(SELLER_ONE))
                .findFirst().orElseThrow();
        InitiatePaymentCommand.VendorGrossAmount sellerTwo = command.vendorGrossAmounts().stream()
                .filter(v -> v.vendorId().equals(SELLER_TWO))
                .findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("200.00").compareTo(sellerOne.grossAmount()));
        assertEquals(0, new BigDecimal("50.00").compareTo(sellerTwo.grossAmount()));
    }

    @Test
    void receivesAuthenticatedOrderCreatedAndMapsCustomerId() {
        when(paymentRepositoryPort.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

        UUID customerId = UUID.randomUUID();
        OrderCreatedEventPayload payload = new OrderCreatedEventPayload(
                ORDER_ID,
                "EKK-20260813-AB12",
                customerId,
                "customer@example.com",
                new BigDecimal("250.00"),
                "CONFIRMED",
                LocalDateTime.of(2026, 8, 13, 12, 0),
                List.of());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                payload);

        ArgumentCaptor<InitiatePaymentCommand> captor = ArgumentCaptor.forClass(InitiatePaymentCommand.class);
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(initiatePaymentUseCase).execute(captor.capture()));

        InitiatePaymentCommand command = captor.getValue();
        assertEquals(customerId, command.customerId());
        assertNull(command.guestEmail());
        assertEquals("customer@example.com", command.customerEmail());
    }

    @Test
    void ignoresDuplicateOrderCreatedWhenPaymentAlreadyExists() {
        when(paymentRepositoryPort.findByOrderId(ORDER_ID))
                .thenReturn(Optional.of(Payment.initiate(
                        ORDER_ID, null, "guest@example.com", new BigDecimal("250.00"), "USD")));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                guestPayload());

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(paymentRepositoryPort).findByOrderId(ORDER_ID));

        verify(initiatePaymentUseCase, never()).execute(any());
    }

    private OrderCreatedEventPayload guestPayload() {
        return new OrderCreatedEventPayload(
                ORDER_ID,
                "EKK-20260813-AB12",
                null,
                "guest@example.com",
                new BigDecimal("250.00"),
                "CONFIRMED",
                LocalDateTime.of(2026, 8, 13, 12, 0),
                List.of(
                        new OrderCreatedEventPayload.OrderItemPayload(
                                VARIANT_ONE, UUID.randomUUID(), 2, SELLER_ONE,
                                new BigDecimal("100.00"), new BigDecimal("200.00")),
                        new OrderCreatedEventPayload.OrderItemPayload(
                                VARIANT_TWO, UUID.randomUUID(), 1, SELLER_TWO,
                                new BigDecimal("50.00"), new BigDecimal("50.00"))));
    }
}
