package com.ekko.payment_service.domain.port.in;

import com.ekko.payment_service.domain.model.Payment;

import java.util.UUID;

public interface GetPaymentByOrderIdUseCase {

    Payment execute(UUID orderId, UUID keycloakId, String guestEmail);
}