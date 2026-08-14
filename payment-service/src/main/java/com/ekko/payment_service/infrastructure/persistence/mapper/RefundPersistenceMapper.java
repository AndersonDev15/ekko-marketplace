package com.ekko.payment_service.infrastructure.persistence.mapper;

import com.ekko.payment_service.domain.model.Refund;
import com.ekko.payment_service.infrastructure.persistence.entity.RefundEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RefundPersistenceMapper {

    @Mapping(target = "payment", ignore = true)
    RefundEntity toEntity(Refund refund);

    default Refund toDomain(RefundEntity entity) {
        return Refund.restore(
                entity.getId(),
                entity.getPayment().getId(),
                entity.getStripeRefundId(),
                entity.getAmount(),
                entity.getReason(),
                entity.getStatus(),
                entity.getRequestedBy(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}