package com.ekko.payment_service.domain.port.out;

import com.ekko.payment_service.domain.model.Refund;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface RefundRepositoryPort {

    Refund save(Refund refund);

    BigDecimal sumSucceededAmountByPaymentId(UUID paymentId);

    Optional<Refund> findByStripeRefundId(String stripeRefundId);

    Optional<Refund> findById(UUID id);

}