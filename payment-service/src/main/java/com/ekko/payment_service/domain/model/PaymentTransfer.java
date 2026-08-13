package com.ekko.payment_service.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentTransfer {

    private final UUID id;
    private final UUID paymentId;
    private final UUID vendorId;
    private String stripeTransferId;
    private final BigDecimal amount;
    private final String currency;
    private TransferStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private PaymentTransfer(UUID id, UUID paymentId, UUID vendorId, String stripeTransferId,
                            BigDecimal amount, String currency, TransferStatus status,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.paymentId = paymentId;
        this.vendorId = vendorId;
        this.stripeTransferId = stripeTransferId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PaymentTransfer initiate(UUID paymentId, UUID vendorId,
                                           BigDecimal amount, String currency) {
        LocalDateTime now = LocalDateTime.now();
        return new PaymentTransfer(
                null,
                paymentId,
                vendorId,
                null,
                amount,
                currency,
                TransferStatus.PENDING,
                now,
                now);
    }

    public static PaymentTransfer restore(UUID id, UUID paymentId, UUID vendorId,
                                          String stripeTransferId,
                                          BigDecimal amount, String currency,
                                          TransferStatus status,
                                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new PaymentTransfer(
                id,
                paymentId,
                vendorId,
                stripeTransferId,
                amount,
                currency,
                status,
                createdAt,
                updatedAt);
    }

    public void markSucceeded(String stripeTransferId) {
        this.stripeTransferId = stripeTransferId;
        this.status = TransferStatus.SUCCEEDED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = TransferStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getVendorId() {
        return vendorId;
    }

    public String getStripeTransferId() {
        return stripeTransferId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}