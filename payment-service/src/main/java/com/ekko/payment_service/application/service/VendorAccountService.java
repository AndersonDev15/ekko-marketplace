package com.ekko.payment_service.application.service;

import com.ekko.payment_service.domain.exception.VendorAccountAlreadyActiveException;
import com.ekko.payment_service.domain.exception.VendorAccountAlreadyExistsException;
import com.ekko.payment_service.application.exception.VendorAccountNotFoundException;
import com.ekko.payment_service.domain.command.CreateVendorAccountCommand;
import com.ekko.payment_service.domain.model.VendorAccountResult;
import com.ekko.payment_service.domain.enums.VendorAccountStatus;
import com.ekko.payment_service.domain.model.VendorStripeAccount;
import com.ekko.payment_service.domain.port.in.CreateVendorAccountUseCase;
import com.ekko.payment_service.domain.port.in.RefreshOnboardingLinkUseCase;
import com.ekko.payment_service.domain.port.out.PaymentGatewayPort;
import com.ekko.payment_service.domain.port.out.VendorStripeAccountRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class VendorAccountService implements CreateVendorAccountUseCase, RefreshOnboardingLinkUseCase {

    private final VendorStripeAccountRepositoryPort vendorStripeAccountRepositoryPort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final VendorAccountTransactionService vendorAccountTransactionService;
    private final String onboardingRefreshUrl;
    private final String onboardingReturnUrl;

    public VendorAccountService(
            VendorStripeAccountRepositoryPort vendorStripeAccountRepositoryPort,
            PaymentGatewayPort paymentGatewayPort,
            VendorAccountTransactionService vendorAccountTransactionService,
            @Value("${app.stripe-onboarding.refresh-url}") String onboardingRefreshUrl,
            @Value("${app.stripe-onboarding.return-url}") String onboardingReturnUrl) {
        this.vendorStripeAccountRepositoryPort = vendorStripeAccountRepositoryPort;
        this.paymentGatewayPort = paymentGatewayPort;
        this.vendorAccountTransactionService = vendorAccountTransactionService;
        this.onboardingRefreshUrl = onboardingRefreshUrl;
        this.onboardingReturnUrl = onboardingReturnUrl;
    }

    @Override
    public VendorAccountResult execute(CreateVendorAccountCommand command) {
        if (vendorStripeAccountRepositoryPort.findByVendorId(command.vendorId()).isPresent()) {
            throw new VendorAccountAlreadyExistsException(command.vendorId());
        }

        String stripeAccountId = paymentGatewayPort.createConnectedAccount(command.country(), command.email());

        VendorStripeAccount account = VendorStripeAccount.initiate(command.vendorId(), stripeAccountId);

        String onboardingUrl = paymentGatewayPort.createAccountLink(
                stripeAccountId,
                onboardingRefreshUrl,
                onboardingReturnUrl);

        VendorStripeAccount persisted = vendorAccountTransactionService.commitVendorAccount(account);
        return new VendorAccountResult(persisted, onboardingUrl);
    }

    @Override
    public String execute(UUID vendorId) {
        VendorStripeAccount account = vendorStripeAccountRepositoryPort.findByVendorId(vendorId)
                .orElseThrow(() -> new VendorAccountNotFoundException(vendorId));

        if (account.getAccountStatus() == VendorAccountStatus.ACTIVE) {
            throw new VendorAccountAlreadyActiveException(vendorId);
        }

        return paymentGatewayPort.createAccountLink(
                account.getStripeAccountId(),
                onboardingRefreshUrl,
                onboardingReturnUrl);
    }
}