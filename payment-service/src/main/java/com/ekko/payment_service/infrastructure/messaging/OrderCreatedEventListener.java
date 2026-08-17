package com.ekko.payment_service.infrastructure.messaging;

import com.ekko.payment_service.domain.exception.VendorAccountNotActiveException;
import com.ekko.payment_service.application.exception.VendorAccountNotFoundException;
import com.ekko.payment_service.domain.command.InitiatePaymentCommand;
import com.ekko.payment_service.domain.port.in.InitiatePaymentUseCase;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.infrastructure.config.RabbitMQConfig;
import com.ekko.payment_service.infrastructure.messaging.dto.OrderCreatedEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedEventListener {

    private static final String CURRENCY = "USD";

    private final InitiatePaymentUseCase initiatePaymentUseCase;
    private final PaymentRepositoryPort paymentRepositoryPort;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void onOrderCreated(OrderCreatedEventPayload payload) {
        if (paymentRepositoryPort.findByOrderId(payload.orderId()).isPresent()) {
            log.warn("Ignoring duplicate order.created for orderId {}: a payment already exists", payload.orderId());
            return;
        }

        InitiatePaymentCommand command = new InitiatePaymentCommand(
                payload.orderId(),
                payload.customerId(),
                payload.customerId() == null ? payload.customerEmail() : null,
                payload.customerEmail(),
                payload.total(),
                CURRENCY,
                groupVendorGrossAmounts(payload.items()));

        try {
            initiatePaymentUseCase.execute(command);
        } catch (VendorAccountNotFoundException e) {
            log.error("Dropping order.created for orderId {}: vendor {} has no Stripe account",
                    payload.orderId(), e.getVendorId());
        } catch (VendorAccountNotActiveException e) {
            log.error("Dropping order.created for orderId {}: vendor {} has no active Stripe account",
                    payload.orderId(), e.getVendorId());
        }
        // Known limitation: unexpected/infrastructure exceptions (e.g. wrapped StripeException, DB errors)
        // propagate to RabbitMQ and, without a configured dead-letter-queue, the default container factory
        // requeues them indefinitely. Business failures above are dropped on purpose so a single misconfigured
        // vendor does not block the queue.
    }

    private List<InitiatePaymentCommand.VendorGrossAmount> groupVendorGrossAmounts(
            List<OrderCreatedEventPayload.OrderItemPayload> items) {
        Map<UUID, BigDecimal> totalsByVendor = new LinkedHashMap<>();
        for (OrderCreatedEventPayload.OrderItemPayload item : items) {
            totalsByVendor.merge(item.sellerKeycloakId(), item.subtotal(), BigDecimal::add);
        }
        return totalsByVendor.entrySet().stream()
                .map(entry -> new InitiatePaymentCommand.VendorGrossAmount(entry.getKey(), entry.getValue()))
                .toList();
    }
}