package com.ekko.payment_service.infrastructure.gateway;

import com.ekko.payment_service.domain.enums.RefundReason;
import com.ekko.payment_service.domain.port.out.PaymentGatewayPort;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;

// V1 — siguen siendo necesarios para Customer, PaymentIntent, Refund y Transfer
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.Transfer;

import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.TransferCreateParams;

// V2 — Connected Accounts y Account Links
import com.stripe.model.v2.core.Account;
import com.stripe.model.v2.core.AccountLink;
import com.stripe.param.v2.core.AccountCreateParams;
import com.stripe.param.v2.core.AccountLinkCreateParams;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Service
@Slf4j
public class StripeGatewayAdapter implements PaymentGatewayPort {

    private static final BigDecimal MINOR_UNITS_MULTIPLIER = new BigDecimal("100");
    private final StripeClient stripeClient;

    public StripeGatewayAdapter(StripeClient stripeClient) {
        this.stripeClient = stripeClient;
    }

    @Override
    public String resolveOrCreateCustomer(String existingStripeCustomerId, String email) {
        if (existingStripeCustomerId != null) {
            return existingStripeCustomerId;
        }
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(email)
                    .build();
            return Customer.create(params).getId();
        } catch (StripeException e) {
            throw new PaymentGatewayException("Failed to resolve or create Stripe customer", e);
        }
    }

    @Override
    public String createPaymentIntent(BigDecimal amount, String currency, String stripeCustomerId, String transferGroup) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(toMinorUnits(amount))
                    .setCurrency(currency.toLowerCase(Locale.ROOT))
                    .setCustomer(stripeCustomerId)
                    .setTransferGroup(transferGroup)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build())
                    .build();
            return PaymentIntent.create(params).getId();
        } catch (StripeException e) {
            throw new PaymentGatewayException("Failed to create PaymentIntent", e);
        }
    }

    @Override
    public String createTransfer(BigDecimal amount, String currency, String stripeAccountId, String transferGroup) {
        try {
            TransferCreateParams params = TransferCreateParams.builder()
                    .setAmount(toMinorUnits(amount))
                    .setCurrency(currency.toLowerCase(Locale.ROOT))
                    .setDestination(stripeAccountId)
                    .setTransferGroup(transferGroup)
                    .build();
            return Transfer.create(params).getId();
        } catch (StripeException e) {
            throw new PaymentGatewayException("Failed to create Stripe Transfer", e);
        }
    }

    @Override
    public String createRefund(String paymentIntentId, BigDecimal amount, RefundReason reason) {
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId)
                    .setAmount(toMinorUnits(amount))
                    .setReason(RefundCreateParams.Reason.valueOf(reason.name()))
                    .build();
            return Refund.create(params).getId();
        } catch (StripeException e) {
            throw new PaymentGatewayException("Failed to create Stripe Refund", e);
        }
    }

    @Override
    public String createConnectedAccount(String country, String email) {
        try {
            AccountCreateParams.Identity identity =
                    AccountCreateParams.Identity.builder()
                            .setCountry(country)
                            .build();

            AccountCreateParams params =
                    AccountCreateParams.builder()
                            .setContactEmail(email)
                            .setIdentity(identity)
                            .build();

            Account account = stripeClient.v2()
                    .core()
                    .accounts()
                    .create(params);

            return account.getId();

        } catch (StripeException e) {
            log.error(
                    "Stripe V2 error creating Connected Account: status={}, code={}, message={}",
                    e.getStatusCode(),
                    e.getCode(),
                    e.getMessage(),
                    e
            );

            throw new PaymentGatewayException(
                    "Failed to create Stripe Connected Account", e);
        }
    }

    @Override
    public String createAccountLink(
            String stripeAccountId,
            String refreshUrl,
            String returnUrl) {

        try {
            AccountLinkCreateParams.UseCase.AccountOnboarding onboarding =
                    AccountLinkCreateParams.UseCase.AccountOnboarding.builder()
                            .setRefreshUrl(refreshUrl)
                            .setReturnUrl(returnUrl)
                            .build();

            AccountLinkCreateParams.UseCase useCase =
                    AccountLinkCreateParams.UseCase.builder()
                            .setType(AccountLinkCreateParams.UseCase.Type.ACCOUNT_ONBOARDING)
                            .setAccountOnboarding(onboarding)
                            .build();

            AccountLinkCreateParams params =
                    AccountLinkCreateParams.builder()
                            .setAccount(stripeAccountId)
                            .setUseCase(useCase)
                            .build();

            AccountLink accountLink = stripeClient.v2()
                    .core()
                    .accountLinks()
                    .create(params);

            return accountLink.getUrl();

        } catch (StripeException e) {
            log.error(
                    "Stripe V2 error creating Account Link: status={}, code={}, message={}",
                    e.getStatusCode(),
                    e.getCode(),
                    e.getMessage(),
                    e
            );

            throw new PaymentGatewayException(
                    "Failed to create Stripe Account Link", e);
        }
    }

    private long toMinorUnits(BigDecimal amount) {
        return amount.multiply(MINOR_UNITS_MULTIPLIER)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }
}
