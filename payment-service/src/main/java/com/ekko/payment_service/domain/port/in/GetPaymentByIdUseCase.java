package com.ekko.payment_service.domain.port.in;

import com.ekko.payment_service.domain.model.Payment;

import java.util.UUID;

public interface GetPaymentByIdUseCase {

    Payment execute(UUID paymentId, UUID keycloakId, String guestEmail);
}