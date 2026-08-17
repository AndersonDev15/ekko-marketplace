package com.ekko.payment_service.application.service;

import com.ekko.payment_service.domain.enums.TransferStatus;
import com.ekko.payment_service.domain.event.TransferFailedEvent;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.PaymentTransfer;
import com.ekko.payment_service.domain.model.VendorAllocation;
import com.ekko.payment_service.domain.model.VendorStripeAccount;
import com.ekko.payment_service.domain.port.in.ProcessTransfersUseCase;
import com.ekko.payment_service.domain.port.out.PaymentEventPublisherPort;
import com.ekko.payment_service.domain.port.out.PaymentGatewayPort;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.domain.port.out.PaymentTransferRepositoryPort;
import com.ekko.payment_service.domain.port.out.VendorStripeAccountRepositoryPort;
import com.ekko.payment_service.infrastructure.gateway.PaymentGatewayException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService implements ProcessTransfersUseCase {

    private final PaymentTransferRepositoryPort paymentTransferRepositoryPort;
    private final PaymentRepositoryPort paymentRepositoryPort;
    private final VendorStripeAccountRepositoryPort vendorStripeAccountRepositoryPort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final PaymentEventPublisherPort paymentEventPublisherPort;

    @Override
    public void execute(UUID paymentId) {
        // Idempotency: if any transfer already exists for this payment, treat the process
        // as already run and do not reprocess anything. NOTE: this deliberately does NOT
        // retry only the FAILED vendors — manual retry is a future feature out of scope.
        // When that retry is built, this guard should become "skip only vendors that already
        // have a transfer row (SUCCEEDED or FAILED) and process the missing ones".
        if (!paymentTransferRepositoryPort.findByPaymentId(paymentId).isEmpty()) {
            return;
        }

        Payment payment = paymentRepositoryPort.findById(paymentId)
                .orElseThrow(() -> new IllegalStateException("Payment not found for id " + paymentId));

        for (VendorAllocation allocation : payment.getAllocations()) {
            PaymentTransfer saved = paymentTransferRepositoryPort.save(transferFor(payment, allocation));
            if (saved.getStatus() == TransferStatus.FAILED) {
                paymentEventPublisherPort.publishTransferFailed(toEvent(payment, saved));
            }
        }
    }

    private TransferFailedEvent toEvent(Payment payment, PaymentTransfer transfer) {
        return new TransferFailedEvent(
                transfer.getId(),
                transfer.getPaymentId(),
                payment.getOrderId(),
                transfer.getVendorId(),
                transfer.getAmount(),
                transfer.getCurrency(),
                LocalDateTime.now());
    }

    private PaymentTransfer transferFor(Payment payment, VendorAllocation allocation) {
        VendorStripeAccount account = vendorStripeAccountRepositoryPort
                .findByVendorId(allocation.vendorId())
                .orElse(null);

        if (account == null || !account.isChargesEnabled()) {
            PaymentTransfer transfer = PaymentTransfer.initiate(
                    payment.getId(),
                    allocation.vendorId(),
                    allocation.netAmount(),
                    payment.getCurrency());
            transfer.markFailed();
            return transfer;
        }
        return attemptTransfer(payment, allocation, account);
    }

    private PaymentTransfer attemptTransfer(Payment payment, VendorAllocation allocation,
                                            VendorStripeAccount account) {
        PaymentTransfer transfer = PaymentTransfer.initiate(
                payment.getId(),
                allocation.vendorId(),
                allocation.netAmount(),
                payment.getCurrency());
        try {
            String stripeTransferId = paymentGatewayPort.createTransfer(
                    allocation.netAmount(),
                    payment.getCurrency(),
                    account.getStripeAccountId(),
                    payment.getId().toString());
            transfer.markSucceeded(stripeTransferId);
        } catch (PaymentGatewayException e) {
            transfer.markFailed();
        }
        return transfer;
    }
}