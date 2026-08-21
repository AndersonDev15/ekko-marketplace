package com.ekko.order_service.application.service;

import com.ekko.order_service.application.exception.OrderNotFoundException;
import com.ekko.order_service.domain.enums.OrderStatus;
import com.ekko.order_service.domain.event.OrderConfirmedEvent;
import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.model.PaymentData;
import com.ekko.order_service.domain.port.in.PaymentCallbackUseCase;
import com.ekko.order_service.domain.port.out.OrderEventPublisherPort;
import com.ekko.order_service.domain.port.out.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderPaymentCallbackService implements PaymentCallbackUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final OrderTransactionService orderTransactionService;
    private final OrderCancellationService orderCancellationService;
    private final OrderEventPublisherPort orderEventPublisherPort;

    @Override
    public Order onPaymentCompleted(UUID orderId, PaymentData paymentData) {
        Order order = findOrder(orderId);

        if (order.getStatus() == OrderStatus.CONFIRMED
                || order.getStatus() == OrderStatus.CANCELLED) {
            return order;
        }

        order.confirm();

        Order saved = orderTransactionService.confirmOrder(order, paymentData);

        orderEventPublisherPort.publishOrderConfirmed(toConfirmedEvent(saved));

        return saved;
    }

    @Override
    public Order onPaymentFailed(UUID orderId) {
        Order order = findOrder(orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            return order;
        }

        return orderCancellationService.cancelBySystem(order.getOrderNumber());
    }

    private Order findOrder(UUID orderId) {
        return orderRepositoryPort.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId.toString()));
    }

    private OrderConfirmedEvent toConfirmedEvent(Order saved) {
        return new OrderConfirmedEvent(
                saved.getId(),
                saved.getOrderNumber(),
                saved.getCustomerId(),
                saved.getCustomerEmail(),
                saved.getItems().stream()
                        .map(item -> new OrderConfirmedEvent.OrderItemConfirmed(
                                item.id(),
                                item.productId(),
                                item.variantId(),
                                item.quantity(),
                                item.sellerKeycloakId(),
                                item.subtotal()))
                        .toList(),
                saved.getUpdatedAt()
        );
    }
}