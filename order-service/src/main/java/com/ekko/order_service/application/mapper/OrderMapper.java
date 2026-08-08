package com.ekko.order_service.application.mapper;

import com.ekko.order_service.application.dto.OrderDetailResponse;
import com.ekko.order_service.application.dto.OrderSummaryResponse;
import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.model.OrderAddress;
import com.ekko.order_service.domain.model.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

    public OrderSummaryResponse toSummaryResponse(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotal(),
                order.getCreatedAt(),
                order.getItems().size());
    }

    public OrderDetailResponse toDetailResponse(Order order) {
        List<OrderDetailResponse.OrderItemResponse> items = order.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        OrderDetailResponse.OrderAddressResponse address =
                order.getAddress() != null ? toAddressResponse(order.getAddress()) : null;

        return new OrderDetailResponse(
                order.getId(),
                order.getCustomerId(),
                order.getGuestEmail(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getSubtotal(),
                order.getShippingCost(),
                order.getDiscount(),
                order.getTotal(),
                order.getNotes(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items,
                address);
    }

    private OrderDetailResponse.OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderDetailResponse.OrderItemResponse(
                item.id(),
                item.variantId(),
                item.productId(),
                item.productNameSnapshot(),
                item.variantSnapshot(),
                item.sellerKeycloakId(),
                item.sellerNameSnapshot(),
                item.priceSnapshot(),
                item.quantity(),
                item.subtotal(),
                item.imageUrlSnapshot());
    }

    private OrderDetailResponse.OrderAddressResponse toAddressResponse(OrderAddress address) {
        return new OrderDetailResponse.OrderAddressResponse(
                address.id(),
                address.fullName(),
                address.phone(),
                address.addressLine(),
                address.city(),
                address.state(),
                address.country(),
                address.postalCode());
    }
}