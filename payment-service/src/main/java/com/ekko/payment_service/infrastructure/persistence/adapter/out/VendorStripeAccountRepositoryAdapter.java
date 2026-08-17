package com.ekko.payment_service.infrastructure.persistence.adapter.out;

import com.ekko.payment_service.domain.model.VendorStripeAccount;
import com.ekko.payment_service.domain.port.out.VendorStripeAccountRepositoryPort;
import com.ekko.payment_service.infrastructure.persistence.entity.VendorStripeAccountEntity;
import com.ekko.payment_service.infrastructure.persistence.mapper.VendorStripeAccountPersistenceMapper;
import com.ekko.payment_service.infrastructure.persistence.repository.VendorStripeAccountJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VendorStripeAccountRepositoryAdapter implements VendorStripeAccountRepositoryPort {

    private final VendorStripeAccountJpaRepository vendorStripeAccountJpaRepository;
    private final VendorStripeAccountPersistenceMapper vendorStripeAccountPersistenceMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<VendorStripeAccount> findByVendorId(UUID vendorId) {
        return vendorStripeAccountJpaRepository.findByVendorId(vendorId)
                .map(vendorStripeAccountPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VendorStripeAccount> findByStripeAccountId(String stripeAccountId) {
        return vendorStripeAccountJpaRepository.findByStripeAccountId(stripeAccountId)
                .map(vendorStripeAccountPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public VendorStripeAccount save(VendorStripeAccount account) {
        VendorStripeAccountEntity entity = vendorStripeAccountPersistenceMapper.toEntity(account);
        VendorStripeAccountEntity saved = vendorStripeAccountJpaRepository.save(entity);
        return vendorStripeAccountPersistenceMapper.toDomain(saved);
    }
}