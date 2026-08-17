package com.ekko.order_service.infrastructure.messaging.rabbit;

import com.ekko.order_service.domain.port.in.PaymentCallbackUseCase;
import com.ekko.order_service.infrastructure.messaging.dto.PaymentCompletedEventPayload;
import com.ekko.order_service.infrastructure.messaging.dto.PaymentFailedEventPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @Mock
    private PaymentCallbackUseCase paymentCallbackUseCase;

    private PaymentEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new PaymentEventListener(paymentCallbackUseCase);
    }

    @Test
    void delegatesPaymentCompletedToUseCaseWithMappedPaymentData() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("200.00");
        LocalDateTime completedAt = LocalDateTime.of(2025, 8, 9, 13, 0);
        PaymentCompletedEventPayload payload = new PaymentCompletedEventPayload(
                paymentId, orderId, UUID.randomUUID(), amount, "USD", completedAt);

        listener.onPaymentCompleted(payload);

        verify(paymentCallbackUseCase).onPaymentCompleted(eq(orderId), argThat(
                data -> data.paymentId().equals(paymentId)
                        && data.amount().compareTo(amount) == 0
                        && data.currency().equals("USD")
                        && data.paidAt().equals(completedAt)));
    }

    @Test
    void delegatesPaymentFailedToUseCase() {
        UUID orderId = UUID.randomUUID();
        PaymentFailedEventPayload payload = new PaymentFailedEventPayload(
                UUID.randomUUID(), orderId, UUID.randomUUID(), "CARD_DECLINED", LocalDateTime.now());

        listener.onPaymentFailed(payload);

        verify(paymentCallbackUseCase).onPaymentFailed(orderId);
    }

    @Test
    void mapsCompletedPayloadFields() {
        UUID paymentId = UUID.randomUUID();
        LocalDateTime completedAt = LocalDateTime.of(2025, 8, 9, 13, 0);
        PaymentCompletedEventPayload payload = new PaymentCompletedEventPayload(
                paymentId, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("99.90"), "USD", completedAt);

        assertEquals(paymentId, payload.paymentId());
        assertEquals("USD", payload.currency());
        assertEquals(0, new BigDecimal("99.90").compareTo(payload.amount()));
        assertEquals(completedAt, payload.completedAt());
    }
}