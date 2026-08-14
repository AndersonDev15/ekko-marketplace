package com.ekko.payment_service.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Payment {

    private final UUID id;
    private final UUID orderId;
    private final UUID customerId;
    private final String guestEmail;
    private String paymentIntentId;
    private final BigDecimal amount;
    private final String currency;
    private PaymentStatus status;
    private String stripeCustomerId;
    private String paymentMethodType;
    private String paymentMethodLast4;
    private LocalDateTime paidAt;
    private final List<VendorAllocation> allocations = new ArrayList<>();
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Payment(UUID id, UUID orderId, UUID customerId, String guestEmail,
                    BigDecimal amount, String currency) {
        this.id = id;
        this.orderId = orderId;
        this.customerId = customerId;
        this.guestEmail = guestEmail;
        this.amount = amount;
        this.currency = currency;
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    private Payment(UUID id, UUID orderId, UUID customerId, String guestEmail,
                    String paymentIntentId, BigDecimal amount, String currency,
                    PaymentStatus status, String stripeCustomerId,
                    String paymentMethodType, String paymentMethodLast4,
                    LocalDateTime paidAt,
                    List<VendorAllocation> allocations,
                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.customerId = customerId;
        this.guestEmail = guestEmail;
        this.paymentIntentId = paymentIntentId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.stripeCustomerId = stripeCustomerId;
        this.paymentMethodType = paymentMethodType;
        this.paymentMethodLast4 = paymentMethodLast4;
        this.paidAt = paidAt;
        if (allocations != null) {
            this.allocations.addAll(allocations);
        }
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

public static Payment initiate(UUID orderId, UUID customerId, String guestEmail,
                                BigDecimal amount, String currency) {
        return new Payment(
                null,
                orderId,
                customerId,
                guestEmail,
                amount,
                currency);
    }

    public static Payment restore(UUID id, UUID orderId, UUID customerId, String guestEmail,
                                  String paymentIntentId, BigDecimal amount, String currency,
                                  PaymentStatus status, String stripeCustomerId,
                                  String paymentMethodType, String paymentMethodLast4,
                                  LocalDateTime paidAt,
                                  List<VendorAllocation> allocations,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Payment(
                id,
                orderId,
                customerId,
                guestEmail,
                paymentIntentId,
                amount,
                currency,
                status,
                stripeCustomerId,
                paymentMethodType,
                paymentMethodLast4,
                paidAt,
                allocations,
                createdAt,
                updatedAt);
    }

    public void attachPaymentIntent(String paymentIntentId, String stripeCustomerId) {
        this.paymentIntentId = paymentIntentId;
        this.stripeCustomerId = stripeCustomerId;
    }

    public void markSucceeded(String paymentMethodType, String paymentMethodLast4,
                              LocalDateTime paidAt) {
        this.status = PaymentStatus.SUCCEEDED;
        this.paymentMethodType = paymentMethodType;
        this.paymentMethodLast4 = paymentMethodLast4;
        this.paidAt = paidAt;
        this.updatedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markCancelled() {
        this.status = PaymentStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markRefunded() {
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = LocalDateTime.now();
    }

    public void addAllocations(List<VendorAllocation> newAllocations) {
        this.allocations.addAll(newAllocations);
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public String getPaymentMethodType() {
        return paymentMethodType;
    }

    public String getPaymentMethodLast4() {
        return paymentMethodLast4;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public List<VendorAllocation> getAllocations() {
        return List.copyOf(allocations);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
