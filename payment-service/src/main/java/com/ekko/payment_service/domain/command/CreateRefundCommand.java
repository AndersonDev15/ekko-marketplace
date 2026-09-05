package com.ekko.payment_service.domain.command;

import com.ekko.payment_service.domain.enums.RefundReason;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateRefundCommand(
        UUID paymentId,
        BigDecimal amount,
        RefundReason reason,
        UUID requestedBy,
        String guestEmail,
        String notes
) {}