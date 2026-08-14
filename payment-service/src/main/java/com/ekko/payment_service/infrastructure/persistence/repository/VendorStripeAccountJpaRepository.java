package com.ekko.payment_service.infrastructure.persistence.repository;

import com.ekko.payment_service.infrastructure.persistence.entity.VendorStripeAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VendorStripeAccountJpaRepository extends JpaRepository<VendorStripeAccountEntity, UUID> {

    Optional<VendorStripeAccountEntity> findByVendorId(UUID vendorId);

    Optional<VendorStripeAccountEntity> findByStripeAccountId(String stripeAccountId);
}