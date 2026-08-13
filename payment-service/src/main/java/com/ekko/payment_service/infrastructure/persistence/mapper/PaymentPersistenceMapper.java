package com.ekko.payment_service.infrastructure.persistence.mapper;

import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.VendorAllocation;
import com.ekko.payment_service.infrastructure.persistence.entity.PaymentEntity;
import com.ekko.payment_service.infrastructure.persistence.entity.VendorAllocationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PaymentPersistenceMapper {

    PaymentEntity toEntity(Payment payment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "payment", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    VendorAllocationEntity toAllocationEntity(VendorAllocation allocation);

    VendorAllocation toAllocationDomain(VendorAllocationEntity entity);

    default Payment toDomain(PaymentEntity entity) {
        List<VendorAllocation> allocations = entity.getAllocations() == null
                ? List.of()
                : entity.getAllocations().stream()
                        .map(this::toAllocationDomain)
                        .toList();
        return Payment.restore(
                entity.getId(),
                entity.getOrderId(),
                entity.getCustomerId(),
                entity.getGuestEmail(),
                entity.getPaymentIntentId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getStripeCustomerId(),
                entity.getPaymentMethodType(),
                entity.getPaymentMethodLast4(),
                entity.getPaidAt(),
                allocations,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
