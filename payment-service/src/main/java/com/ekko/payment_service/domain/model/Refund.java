package com.ekko.payment_service.domain.model;

import com.ekko.payment_service.domain.enums.RefundReason;
import com.ekko.payment_service.domain.enums.RefundStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Refund {

    private final UUID id;
    private final UUID paymentId;
    private String stripeRefundId;
    private final BigDecimal amount;
    private final RefundReason reason;
    private RefundStatus status;
    private final UUID requestedBy;
    private final String notes;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Refund(UUID id, UUID paymentId, String stripeRefundId, BigDecimal amount,
                   RefundReason reason, RefundStatus status, UUID requestedBy, String notes,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.paymentId = paymentId;
        this.stripeRefundId = stripeRefundId;
        this.amount = amount;
        this.reason = reason;
        this.status = status;
        this.requestedBy = requestedBy;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Refund initiate(UUID paymentId, BigDecimal amount, RefundReason reason,
                                  UUID requestedBy, String notes) {
        LocalDateTime now = LocalDateTime.now();
        return new Refund(
                null,
                paymentId,
                null,
                amount,
                reason,
                RefundStatus.PENDING,
                requestedBy,
                notes,
                now,
                now);
    }

    public static Refund restore(UUID id, UUID paymentId, String stripeRefundId, BigDecimal amount,
                                 RefundReason reason, RefundStatus status, UUID requestedBy, String notes,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Refund(
                id,
                paymentId,
                stripeRefundId,
                amount,
                reason,
                status,
                requestedBy,
                notes,
                createdAt,
                updatedAt);
    }

    public void attachStripeRefundId(String stripeRefundId) {
        this.stripeRefundId = stripeRefundId;
    }

    public void markSucceeded() {
        this.status = RefundStatus.SUCCEEDED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = RefundStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public String getStripeRefundId() {
        return stripeRefundId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public RefundReason getReason() {
        return reason;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}