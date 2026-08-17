package com.ekko.payment_service.infrastructure.persistence.mapper;

import com.ekko.payment_service.domain.enums.VendorAccountStatus;
import com.ekko.payment_service.domain.model.VendorStripeAccount;
import com.ekko.payment_service.infrastructure.persistence.entity.VendorStripeAccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface VendorStripeAccountPersistenceMapper {

    VendorStripeAccountEntity toEntity(VendorStripeAccount account);

    default VendorStripeAccount toDomain(VendorStripeAccountEntity entity) {
        return VendorStripeAccount.restore(
                entity.getId(),
                entity.getVendorId(),
                entity.getStripeAccountId(),
                VendorAccountStatus.valueOf(entity.getAccountStatus().name()),
                entity.isChargesEnabled(),
                entity.isPayoutsEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}