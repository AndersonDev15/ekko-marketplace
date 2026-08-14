package com.ekko.payment_service.domain.port.in;

import com.ekko.payment_service.domain.model.Payment;

import java.util.UUID;

public interface GetPaymentByIdForAdminUseCase {

    Payment execute(UUID paymentId);
}