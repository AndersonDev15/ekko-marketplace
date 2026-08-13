package com.ekko.payment_service.infrastructure.persistence.mapper;

import com.ekko.payment_service.domain.model.PaymentTransaction;
import com.ekko.payment_service.infrastructure.persistence.entity.PaymentTransactionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PaymentTransactionPersistenceMapper {

    @Mapping(target = "payment", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    PaymentTransactionEntity toEntity(PaymentTransaction transaction);

    default PaymentTransaction toDomain(PaymentTransactionEntity entity) {
        return PaymentTransaction.restore(
                entity.getId(),
                entity.getPayment().getId(),
                entity.getType(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getStripeEventId(),
                entity.getErrorMessage(),
                entity.getCreatedAt());
    }
}