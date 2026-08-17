package com.ekko.order_service.application.mapper;

import com.ekko.order_service.application.dto.OrderDetailResponse;
import com.ekko.order_service.application.dto.OrderSummaryResponse;
import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.model.OrderAddress;
import com.ekko.order_service.domain.model.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrderMapper {

    @Mapping(target = "itemCount", expression = "java(order.getItems().size())")
    OrderSummaryResponse toSummaryResponse(Order order);

    OrderDetailResponse toDetailResponse(Order order);

    OrderDetailResponse.OrderItemResponse toItemResponse(OrderItem item);

    OrderDetailResponse.OrderAddressResponse toAddressResponse(OrderAddress address);
}