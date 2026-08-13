package com.ekko.payment_service.domain.port.out;

import com.ekko.payment_service.domain.model.PaymentTransfer;

import java.util.List;
import java.util.UUID;

public interface PaymentTransferRepositoryPort {

    PaymentTransfer save(PaymentTransfer transfer);

    List<PaymentTransfer> findByPaymentId(UUID paymentId);
}