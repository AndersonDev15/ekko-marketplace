package com.ekko.payment_service.infrastructure.gateway;

import com.ekko.payment_service.domain.enums.RefundReason;
import com.ekko.payment_service.domain.port.out.PaymentGatewayPort;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.Transfer;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.TransferCreateParams;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Service
public class StripeGatewayAdapter implements PaymentGatewayPort {

    private static final BigDecimal MINOR_UNITS_MULTIPLIER = new BigDecimal("100");

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
            AccountCreateParams params = AccountCreateParams.builder()
                    .setType(AccountCreateParams.Type.EXPRESS)
                    .setCountry(country)
                    .setEmail(email)
                    .build();
            return Account.create(params).getId();
        } catch (StripeException e) {
            throw new PaymentGatewayException("Failed to create Stripe Connected Account", e);
        }
    }

    @Override
    public String createAccountLink(String stripeAccountId, String refreshUrl, String returnUrl) {
        try {
            AccountLinkCreateParams params = AccountLinkCreateParams.builder()
                    .setAccount(stripeAccountId)
                    .setRefreshUrl(refreshUrl)
                    .setReturnUrl(returnUrl)
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .build();
            return AccountLink.create(params).getUrl();
        } catch (StripeException e) {
            throw new PaymentGatewayException("Failed to create Stripe Account Link", e);
        }
    }

    private long toMinorUnits(BigDecimal amount) {
        return amount.multiply(MINOR_UNITS_MULTIPLIER)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }
}
