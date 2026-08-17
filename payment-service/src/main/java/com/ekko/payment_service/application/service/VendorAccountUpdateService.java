package com.ekko.payment_service.application.service;

import com.ekko.payment_service.application.exception.VendorAccountNotFoundException;
import com.ekko.payment_service.domain.command.HandleAccountUpdatedCommand;
import com.ekko.payment_service.domain.model.VendorStripeAccount;
import com.ekko.payment_service.domain.port.in.HandleAccountUpdatedUseCase;
import com.ekko.payment_service.domain.port.out.VendorStripeAccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VendorAccountUpdateService implements HandleAccountUpdatedUseCase {

    private final VendorStripeAccountRepositoryPort vendorStripeAccountRepositoryPort;
    private final VendorAccountTransactionService vendorAccountTransactionService;

    @Override
    public VendorStripeAccount execute(HandleAccountUpdatedCommand command) {
        VendorStripeAccount account = vendorStripeAccountRepositoryPort
                .findByStripeAccountId(command.stripeAccountId())
                .orElseThrow(() -> new VendorAccountNotFoundException(command.stripeAccountId()));

        account.applyAccountUpdate(
                command.chargesEnabled(),
                command.payoutsEnabled(),
                command.disabledReasonPresent(),
                command.hasPendingRequirements());

        return vendorAccountTransactionService.updateAccountStatus(account);
    }
}