package com.ekko.payment_service.domain.port.in;

import com.ekko.payment_service.domain.model.PaymentTransaction;

import java.util.List;
import java.util.UUID;

public interface GetTransactionsByPaymentIdForAdminUseCase {

    List<PaymentTransaction> execute(UUID paymentId);
}