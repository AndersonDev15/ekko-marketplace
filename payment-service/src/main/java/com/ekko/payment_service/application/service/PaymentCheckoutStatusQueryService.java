package com.ekko.payment_service.application.service;

import com.ekko.payment_service.domain.enums.PaymentStatus;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.port.in.GetPaymentByOrderIdUseCase;
import com.ekko.payment_service.domain.port.in.GetPaymentCheckoutStatusUseCase;
import com.ekko.payment_service.domain.port.out.PaymentGatewayPort;
import com.ekko.payment_service.domain.port.out.StripePaymentIntentSnapshot;
import com.ekko.payment_service.infrastructure.persistence.adapter.in.web.dto.response.PaymentCheckoutStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentCheckoutStatusQueryService implements GetPaymentCheckoutStatusUseCase {

    private static final Set<String> REUSABLE_PAYMENT_INTENT_STATUSES = Set.of(
            "requires_payment_method",
            "requires_confirmation",
            "requires_action"
    );

    private final GetPaymentByOrderIdUseCase getPaymentByOrderIdUseCase;
    private final PaymentGatewayPort paymentGatewayPort;

    @Override
    public PaymentCheckoutStatusResponse execute(UUID orderId, UUID keycloakId, String guestEmail) {
        Payment payment = getPaymentByOrderIdUseCase.execute(orderId, keycloakId, guestEmail);
        String clientSecret = resolveClientSecret(payment);
        return new PaymentCheckoutStatusResponse(payment.getOrderId(), payment.getStatus(), clientSecret);
    }

    private String resolveClientSecret(Payment payment) {
        boolean eligible = payment.getStatus() == PaymentStatus.PENDING
                || payment.getStatus() == PaymentStatus.FAILED;

        if (!eligible || payment.getPaymentIntentId() == null) {
            return null;
        }

        StripePaymentIntentSnapshot snapshot =
                paymentGatewayPort.retrievePaymentIntent(payment.getPaymentIntentId());

        return REUSABLE_PAYMENT_INTENT_STATUSES.contains(snapshot.status())
                ? snapshot.clientSecret()
                : null;
    }
}