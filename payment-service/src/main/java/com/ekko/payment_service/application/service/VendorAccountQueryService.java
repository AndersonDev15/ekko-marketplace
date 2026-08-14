package com.ekko.payment_service.application.service;

import com.ekko.payment_service.domain.exception.VendorAccountNotFoundException;
import com.ekko.payment_service.domain.model.VendorStripeAccount;
import com.ekko.payment_service.domain.port.in.GetVendorAccountUseCase;
import com.ekko.payment_service.domain.port.out.VendorStripeAccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VendorAccountQueryService implements GetVendorAccountUseCase {

    private final VendorStripeAccountRepositoryPort vendorStripeAccountRepositoryPort;

    @Override
    public VendorStripeAccount execute(UUID vendorId) {
        return vendorStripeAccountRepositoryPort.findByVendorId(vendorId)
                .orElseThrow(() -> new VendorAccountNotFoundException(vendorId));
    }
}