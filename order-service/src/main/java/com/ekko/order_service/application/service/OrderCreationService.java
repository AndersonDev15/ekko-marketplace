package com.ekko.order_service.application.service;

import com.ekko.order_service.domain.exception.InsufficientStockException;
import com.ekko.order_service.domain.exception.InvalidGuestEmailException;
import com.ekko.order_service.application.exception.OrderNumberGenerationException;
import com.ekko.order_service.application.exception.ProductVariantNotFoundException;
import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.event.OrderCreatedEvent;
import com.ekko.order_service.domain.model.OrderDraft;
import com.ekko.order_service.domain.model.OrderItem;
import com.ekko.order_service.domain.enums.OrderStatus;
import com.ekko.order_service.domain.model.ProductVariant;
import com.ekko.order_service.domain.model.StockItem;
import com.ekko.order_service.domain.port.in.CreateOrderUseCase;
import com.ekko.order_service.domain.port.out.OrderEventPublisherPort;
import com.ekko.order_service.domain.port.out.OrderRepositoryPort;
import com.ekko.order_service.domain.port.out.ProductServicePort;
import com.ekko.order_service.domain.service.OrderNumberGenerator;
import com.ekko.order_service.domain.model.OrderTotals;
import com.ekko.order_service.domain.service.OrderTotalsCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderCreationService implements CreateOrderUseCase {

    private static final int ORDER_NUMBER_MAX_ATTEMPTS = 3;

    private final ProductServicePort productServicePort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final OrderTransactionService orderTransactionService;
    private final OrderEventPublisherPort orderEventPublisherPort;
    private final OrderNumberGenerator orderNumberGenerator;
    private final OrderTotalsCalculator orderTotalsCalculator;

    @Override
    public Order execute(OrderDraft draft) {
        validateGuest(draft);
        validateItemsNotEmpty(draft);

        List<UUID> variantIds = draft.items().stream()
                .map(OrderDraft.OrderItemDraft::variantId)
                .toList();

        List<ProductVariant> variants = productServicePort.getVariantsInfo(variantIds);
        Map<UUID, ProductVariant> variantsById = variants.stream()
                .collect(Collectors.toMap(ProductVariant::variantId, Function.identity()));

        List<OrderItem> orderItems = buildItems(draft, variantsById);
        validateStock(orderItems, variantsById);

        OrderTotals totals = orderTotalsCalculator.calculate(orderItems);

        Order order = Order.builder()
                .customerId(draft.customerId())
                .guestEmail(guestEmailFor(draft))
                .status(OrderStatus.PENDING)
                .subtotal(totals.subtotal())
                .shippingCost(totals.shippingCost())
                .discount(totals.discount())
                .total(totals.total())
                .notes(draft.notes())
                .orderNumber(generateUniqueOrderNumber())
                .items(new ArrayList<>(orderItems))
                .address(draft.shippingAddress())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        productServicePort.reserveStock(order.getItems().stream()
                .map(item -> new StockItem(item.variantId(), item.quantity()))
                .toList());

        Order saved = orderTransactionService.commitOrder(order);
        orderEventPublisherPort.publishOrderCreated(toEvent(saved, draft.customerEmail()));
        return saved;
    }

    private OrderCreatedEvent toEvent(Order saved, String customerEmail) {
        return new OrderCreatedEvent(
                saved.getId(),
                saved.getOrderNumber(),
                saved.getCustomerId(),
                customerEmail,
                saved.getTotal(),
                saved.getStatus(),
                saved.getCreatedAt(),
                saved.getItems().stream()
                        .map(item -> new OrderCreatedEvent.OrderItemPayload(
                                item.variantId(),
                                item.productId(),
                                item.quantity(),
                                item.sellerKeycloakId(),
                                item.priceSnapshot(),
                                item.subtotal()))
                        .toList());
    }

    private void validateGuest(OrderDraft draft) {
        if (draft.customerId() == null && draft.guestEmail() == null) {
            throw new InvalidGuestEmailException(
                    "Se requiere customerId o guestEmail");
        }
        if (draft.guestEmail() != null && draft.guestEmail().isBlank()) {
            throw new InvalidGuestEmailException(
                    "guestEmail no puede estar vacío");
        }
    }

    private void validateItemsNotEmpty(OrderDraft draft) {
        if (draft.items() == null || draft.items().isEmpty()) {
            throw new ProductVariantNotFoundException(UUID.randomUUID());
        }
    }

    private List<OrderItem> buildItems(
            OrderDraft draft,
            Map<UUID, ProductVariant> variantsById) {

        return draft.items().stream()
                .map(item -> {
                    ProductVariant variant =
                            variantsById.get(item.variantId());
                    if (variant == null) {
                        throw new ProductVariantNotFoundException(item.variantId());
                    }
                    BigDecimal price = variant.price();
                    return new OrderItem(
                            null,
                            variant.variantId(),
                            variant.productId(),
                            variant.productName(),
                            variant.sku(),
                            variant.sellerKeycloakId(),
                            variant.storeName(),
                            price,
                            (int) item.quantity(),
                            price.multiply(BigDecimal.valueOf(item.quantity())),
                            variant.imageUrl());
                })
                .toList();
    }

    private void validateStock(
            List<OrderItem> orderItems,
            Map<UUID, ProductVariant> variantsById) {

        for (OrderItem item : orderItems) {
            ProductVariant variant = variantsById.get(item.variantId());
            long available = variant.availableStock() != null
                    ? variant.availableStock()
                    : 0L;
            if (available < item.quantity()) {
                throw new InsufficientStockException(
                        item.variantId(), item.quantity());
            }
        }
    }

    private String generateUniqueOrderNumber() {
        for (int attempt = 0; attempt < ORDER_NUMBER_MAX_ATTEMPTS; attempt++) {
            String candidate = orderNumberGenerator.generate();
            if (orderRepositoryPort.findByOrderNumber(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new OrderNumberGenerationException(
                "No se generó un orderNumber único tras "
                        + ORDER_NUMBER_MAX_ATTEMPTS + " intentos");
    }

    private String guestEmailFor(OrderDraft draft) {
        return draft.guestEmail() != null
                ? draft.guestEmail().strip()
                : null;
    }
}