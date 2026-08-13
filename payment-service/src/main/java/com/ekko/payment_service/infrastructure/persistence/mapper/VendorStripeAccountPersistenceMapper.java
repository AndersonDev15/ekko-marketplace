package com.ekko.payment_service.infrastructure.persistence.mapper;

import com.ekko.payment_service.domain.model.VendorStripeAccount;
import com.ekko.payment_service.infrastructure.persistence.entity.VendorStripeAccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface VendorStripeAccountPersistenceMapper {

    VendorStripeAccount toDomain(VendorStripeAccountEntity entity);
}
