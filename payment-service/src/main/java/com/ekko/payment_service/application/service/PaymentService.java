package com.ekko.payment_service.application.service;

import com.ekko.payment_service.domain.exception.PaymentAlreadySucceededException;
import com.ekko.payment_service.domain.exception.VendorAccountNotActiveException;
import com.ekko.payment_service.domain.exception.VendorAccountNotFoundException;
import com.ekko.payment_service.domain.model.InitiatePaymentCommand;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.PaymentInitiatedEvent;
import com.ekko.payment_service.domain.model.PaymentStatus;
import com.ekko.payment_service.domain.model.VendorAllocation;
import com.ekko.payment_service.domain.model.VendorStripeAccount;
import com.ekko.payment_service.domain.port.in.InitiatePaymentUseCase;
import com.ekko.payment_service.domain.port.out.PaymentEventPublisherPort;
import com.ekko.payment_service.domain.port.out.PaymentGatewayPort;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.domain.port.out.VendorStripeAccountRepositoryPort;
import com.ekko.payment_service.domain.service.ApplicationFeeCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService implements InitiatePaymentUseCase {

    private final PaymentRepositoryPort paymentRepositoryPort;
    private final VendorStripeAccountRepositoryPort vendorStripeAccountRepositoryPort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final PaymentEventPublisherPort paymentEventPublisherPort;
    private final PaymentTransactionService paymentTransactionService;

    private final ApplicationFeeCalculator applicationFeeCalculator = new ApplicationFeeCalculator();

    @Override
    public Payment execute(InitiatePaymentCommand command) {
        assertNotAlreadySucceeded(command.orderId());

        Payment payment = Payment.initiate(
                command.orderId(),
                command.customerId(),
                command.guestEmail(),
                command.amount(),
                command.currency());

        String stripeCustomerId = resolveStripeCustomer(command);
        attachAllocations(payment, command);
        String paymentIntentId = paymentGatewayPort.createPaymentIntent(
                payment.getAmount(),
                payment.getCurrency(),
                stripeCustomerId,
                payment.getOrderId().toString());
        payment.attachPaymentIntent(paymentIntentId, stripeCustomerId);

        Payment saved = paymentTransactionService.commitPayment(payment);
        paymentEventPublisherPort.publishPaymentInitiated(toEvent(saved));
        return saved;
    }

    private void assertNotAlreadySucceeded(UUID orderId) {
        paymentRepositoryPort.findByOrderIdAndStatus(orderId, PaymentStatus.SUCCEEDED)
                .ifPresent(ignored -> {
                    throw new PaymentAlreadySucceededException(orderId);
                });
    }

    private String resolveStripeCustomer(InitiatePaymentCommand command) {
        String existingStripeCustomerId = null;
        if (command.customerId() != null) {
            existingStripeCustomerId = paymentRepositoryPort
                    .findExistingStripeCustomerId(command.customerId())
                    .orElse(null);
        }
        return paymentGatewayPort.resolveOrCreateCustomer(
                existingStripeCustomerId,
                command.customerEmail());
    }

    private void attachAllocations(Payment payment, InitiatePaymentCommand command) {
        for (InitiatePaymentCommand.VendorGrossAmount vendor : command.vendorGrossAmounts()) {
            VendorStripeAccount account = vendorStripeAccountRepositoryPort
                    .findByVendorId(vendor.vendorId())
                    .orElseThrow(() -> new VendorAccountNotFoundException(vendor.vendorId()));
            if (!account.isChargesEnabled()) {
                throw new VendorAccountNotActiveException(vendor.vendorId());
            }
            VendorAllocation allocation = applicationFeeCalculator.calculate(
                    vendor.vendorId(),
                    vendor.grossAmount());
            payment.addAllocations(List.of(allocation));
        }
    }

    private PaymentInitiatedEvent toEvent(Payment saved) {
        return new PaymentInitiatedEvent(
                saved.getId(),
                saved.getOrderId(),
                saved.getCustomerId(),
                saved.getAmount(),
                saved.getCurrency(),
                saved.getPaymentIntentId(),
                LocalDateTime.now());
    }
}
