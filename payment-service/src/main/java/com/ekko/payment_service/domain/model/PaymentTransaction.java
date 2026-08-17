package com.ekko.payment_service.domain.model;

import com.ekko.payment_service.domain.enums.TransactionStatus;
import com.ekko.payment_service.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentTransaction {

    private final UUID id;
    private final UUID paymentId;
    private final TransactionType type;
    private final BigDecimal amount;
    private final String currency;
    private final TransactionStatus status;
    private final String stripeEventId;
    private final String errorMessage;
    private final LocalDateTime createdAt;

    private PaymentTransaction(UUID id, UUID paymentId, TransactionType type,
                               BigDecimal amount, String currency,
                               TransactionStatus status, String stripeEventId,
                               String errorMessage,
                               LocalDateTime createdAt) {
        this.id = id;
        this.paymentId = paymentId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.stripeEventId = stripeEventId;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
    }

    public static PaymentTransaction initiate(UUID paymentId, TransactionType type,
                                              BigDecimal amount, String currency,
                                              TransactionStatus status, String stripeEventId) {
        return initiate(paymentId, type, amount, currency, status, stripeEventId, null);
    }

    public static PaymentTransaction initiate(UUID paymentId, TransactionType type,
                                              BigDecimal amount, String currency,
                                              TransactionStatus status, String stripeEventId,
                                              String errorMessage) {
        return new PaymentTransaction(
                null,
                paymentId,
                type,
                amount,
                currency,
                status,
                stripeEventId,
                errorMessage,
                LocalDateTime.now());
    }

    public static PaymentTransaction restore(UUID id, UUID paymentId, TransactionType type,
                                             BigDecimal amount, String currency,
                                             TransactionStatus status, String stripeEventId,
                                             LocalDateTime createdAt) {
        return restore(id, paymentId, type, amount, currency, status, stripeEventId, null, createdAt);
    }

    public static PaymentTransaction restore(UUID id, UUID paymentId, TransactionType type,
                                             BigDecimal amount, String currency,
                                             TransactionStatus status, String stripeEventId,
                                             String errorMessage,
                                             LocalDateTime createdAt) {
        return new PaymentTransaction(
                id,
                paymentId,
                type,
                amount,
                currency,
                status,
                stripeEventId,
                errorMessage,
                createdAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getStripeEventId() {
        return stripeEventId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}