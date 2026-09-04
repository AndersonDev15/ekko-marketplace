package com.ekko.payment_service.infrastructure.gateway;

import com.ekko.payment_service.domain.enums.RefundReason;
import com.ekko.payment_service.domain.port.out.PaymentGatewayPort;

import com.ekko.payment_service.domain.port.out.StripePaymentIntentSnapshot;
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
    public String createPaymentIntent(
            BigDecimal amount,
            String currency,
            String stripeCustomerId,
            String transferGroup) {

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(toMinorUnits(amount))
                    .setCurrency(currency.toLowerCase(Locale.ROOT))
                    .setCustomer(stripeCustomerId)
                    .setTransferGroup(transferGroup)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(
                                            PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER
                                    )
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
            // ============================================================
            // 1. Solicitar capability para recibir Stripe Transfers
            // ============================================================

            AccountCreateParams.Configuration.Recipient.Capabilities.StripeBalance.StripeTransfers stripeTransfers =
                    AccountCreateParams.Configuration.Recipient.Capabilities.StripeBalance.StripeTransfers.builder()
                            .setRequested(true)
                            .build();

            AccountCreateParams.Configuration.Recipient.Capabilities.StripeBalance stripeBalance =
                    AccountCreateParams.Configuration.Recipient.Capabilities.StripeBalance.builder()
                            .setStripeTransfers(stripeTransfers)
                            .build();

            AccountCreateParams.Configuration.Recipient.Capabilities capabilities =
                    AccountCreateParams.Configuration.Recipient.Capabilities.builder()
                            .setStripeBalance(stripeBalance)
                            .build();

            // ============================================================
            // 2. Configuración Recipient
            // ============================================================

            AccountCreateParams.Configuration.Recipient recipient =
                    AccountCreateParams.Configuration.Recipient.builder()
                            .setCapabilities(capabilities)
                            .build();

            // ============================================================
            // 3. Configuración Merchant
            // ============================================================

            AccountCreateParams.Configuration.Merchant merchant =
                    AccountCreateParams.Configuration.Merchant.builder()
                            .build();

            // ============================================================
            // 4. Merchant + Recipient
            // ============================================================

            AccountCreateParams.Configuration configuration =
                    AccountCreateParams.Configuration.builder()
                            .setMerchant(merchant)
                            .setRecipient(recipient)
                            .build();

            // ============================================================
            // 5. Responsabilidades
            // ============================================================

            AccountCreateParams.Defaults.Responsibilities responsibilities =
                    AccountCreateParams.Defaults.Responsibilities.builder()
                            .setFeesCollector(
                                    AccountCreateParams.Defaults.Responsibilities.FeesCollector.APPLICATION
                            )
                            .setLossesCollector(
                                    AccountCreateParams.Defaults.Responsibilities.LossesCollector.STRIPE
                            )
                            .build();

            AccountCreateParams.Defaults defaults =
                    AccountCreateParams.Defaults.builder()
                            .setResponsibilities(responsibilities)
                            .build();

            // ============================================================
            // 6. Identidad
            // ============================================================

            AccountCreateParams.Identity identity =
                    AccountCreateParams.Identity.builder()
                            .setCountry(country)
                            .build();

            // ============================================================
            // 7. Crear cuenta V2
            // ============================================================

            AccountCreateParams params =
                    AccountCreateParams.builder()
                            .setContactEmail(email)
                            .setIdentity(identity)
                            .setConfiguration(configuration)
                            .setDashboard(AccountCreateParams.Dashboard.NONE)
                            .setDefaults(defaults)
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
            // El Account Link debe tener las MISMAS configuraciones
            // que la cuenta conectada: Merchant + Recipient.
            AccountLinkCreateParams.UseCase.AccountOnboarding onboarding =
                    AccountLinkCreateParams.UseCase.AccountOnboarding.builder()
                            .setRefreshUrl(refreshUrl)
                            .setReturnUrl(returnUrl)
                            .addConfiguration(
                                    AccountLinkCreateParams.UseCase.AccountOnboarding.Configuration.MERCHANT
                            )
                            .addConfiguration(
                                    AccountLinkCreateParams.UseCase.AccountOnboarding.Configuration.RECIPIENT
                            )
                            .build();

            AccountLinkCreateParams.UseCase useCase =
                    AccountLinkCreateParams.UseCase.builder()
                            .setType(
                                    AccountLinkCreateParams.UseCase.Type.ACCOUNT_ONBOARDING
                            )
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

    @Override
    public String retrievePaymentIntentClientSecret(String paymentIntentId) {
        try {
            return PaymentIntent.retrieve(paymentIntentId).getClientSecret();
        } catch (StripeException e) {
            throw new PaymentGatewayException("Failed to retrieve PaymentIntent " + paymentIntentId, e);
        }
    }

    @Override
    public StripePaymentIntentSnapshot retrievePaymentIntent(String paymentIntentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            return new StripePaymentIntentSnapshot(intent.getStatus(), intent.getClientSecret());
        } catch (StripeException e) {
            throw new PaymentGatewayException("Failed to retrieve PaymentIntent " + paymentIntentId, e);
        }
    }





    private long toMinorUnits(BigDecimal amount) {
        return amount.multiply(MINOR_UNITS_MULTIPLIER)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }
}
