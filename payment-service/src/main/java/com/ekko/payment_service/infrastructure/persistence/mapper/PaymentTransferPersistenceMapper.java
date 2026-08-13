package com.ekko.payment_service.infrastructure.persistence.mapper;

import com.ekko.payment_service.domain.model.PaymentTransfer;
import com.ekko.payment_service.infrastructure.persistence.entity.PaymentTransferEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PaymentTransferPersistenceMapper {

    @Mapping(target = "payment", ignore = true)
    PaymentTransferEntity toEntity(PaymentTransfer transfer);

    default PaymentTransfer toDomain(PaymentTransferEntity entity) {
        return PaymentTransfer.restore(
                entity.getId(),
                entity.getPayment().getId(),
                entity.getVendorId(),
                entity.getStripeTransferId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}