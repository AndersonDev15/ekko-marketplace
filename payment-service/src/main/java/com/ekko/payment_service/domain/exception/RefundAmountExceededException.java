package com.ekko.payment_service.domain.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class RefundAmountExceededException extends RuntimeException {

    private final UUID paymentId;
    private final BigDecimal requestedAmount;
    private final BigDecimal alreadyRefunded;
    private final BigDecimal paymentAmount;

    public RefundAmountExceededException(UUID paymentId, BigDecimal requestedAmount,
                                         BigDecimal alreadyRefunded, BigDecimal paymentAmount) {
        super("Refund amount " + requestedAmount + " for payment " + paymentId
                + " would exceed the payment amount, given " + alreadyRefunded + " already refunded "
                + "(refunded and requested total " + requestedAmount.add(alreadyRefunded)
                + " exceeds payment amount " + paymentAmount + ")");
        this.paymentId = paymentId;
        this.requestedAmount = requestedAmount;
        this.alreadyRefunded = alreadyRefunded;
        this.paymentAmount = paymentAmount;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public BigDecimal getAlreadyRefunded() {
        return alreadyRefunded;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }
}