package com.ekko.payment_service.application.service;

import com.ekko.payment_service.domain.model.VendorStripeAccount;
import com.ekko.payment_service.domain.port.out.VendorStripeAccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VendorAccountTransactionService {

    private final VendorStripeAccountRepositoryPort vendorStripeAccountRepositoryPort;

    @Transactional
    public VendorStripeAccount commitVendorAccount(VendorStripeAccount account) {
        return vendorStripeAccountRepositoryPort.save(account);
    }

    @Transactional
    public VendorStripeAccount updateAccountStatus(VendorStripeAccount account) {
        return vendorStripeAccountRepositoryPort.save(account);
    }
}