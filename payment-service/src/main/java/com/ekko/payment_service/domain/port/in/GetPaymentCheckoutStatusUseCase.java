package com.ekko.payment_service.domain.port.in;

import com.ekko.payment_service.infrastructure.persistence.adapter.in.web.dto.response.PaymentCheckoutStatusResponse;

import java.util.UUID;

public interface GetPaymentCheckoutStatusUseCase {
    PaymentCheckoutStatusResponse execute(UUID orderId, UUID keycloakId, String guestEmail);
}
