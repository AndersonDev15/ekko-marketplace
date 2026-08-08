package com.ekko.order_service.infrastructure.persistence.mapper;

import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.model.OrderAddress;
import com.ekko.order_service.domain.model.OrderItem;
import com.ekko.order_service.infrastructure.persistence.entity.OrderAddressEntity;
import com.ekko.order_service.infrastructure.persistence.entity.OrderEntity;
import com.ekko.order_service.infrastructure.persistence.entity.OrderItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrderPersistenceMapper {

    OrderEntity toEntity(Order order);

    Order toDomain(OrderEntity entity);

    @Mapping(target = "order", ignore = true)
    OrderItemEntity toItemEntity(OrderItem item);

    @Mapping(target = "order", ignore = true)
    OrderAddressEntity toAddressEntity(OrderAddress address);

    OrderItem toItemDomain(OrderItemEntity entity);

    OrderAddress toAddressDomain(OrderAddressEntity entity);
}