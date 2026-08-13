package com.ekko.payment_service.infrastructure.persistence.adapter;

import com.ekko.payment_service.domain.model.VendorStripeAccount;
import com.ekko.payment_service.domain.port.out.VendorStripeAccountRepositoryPort;
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
}
