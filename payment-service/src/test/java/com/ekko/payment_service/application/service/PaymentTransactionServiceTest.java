package com.ekko.payment_service.application.service;

import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.PaymentStatus;
import com.ekko.payment_service.domain.model.PaymentTransaction;
import com.ekko.payment_service.domain.model.TransactionStatus;
import com.ekko.payment_service.domain.model.TransactionType;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.domain.port.out.PaymentTransactionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentTransactionServiceTest {

    private static final String STRIPE_EVENT_ID = "evt_test_123";

    @Mock
    private PaymentRepositoryPort paymentRepositoryPort;
    @Mock
    private PaymentTransactionRepositoryPort paymentTransactionRepositoryPort;

    private PaymentTransactionService service;

    @BeforeEach
    void setUp() {
        service = new PaymentTransactionService(paymentRepositoryPort, paymentTransactionRepositoryPort);
    }

    @Test
    void commitPaymentSavesAndReturnsPayment() {
        Payment payment = payment();
        when(paymentRepositoryPort.save(payment)).thenReturn(payment);

        Payment result = service.commitPayment(payment);

        assertEquals(payment, result);
        verify(paymentRepositoryPort).save(payment);
        verify(paymentTransactionRepositoryPort, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void confirmPaymentSavesPaymentAndTransaction() {
        Payment payment = payment();
        PaymentTransaction transaction = transaction(TransactionType.CHARGE, TransactionStatus.SUCCEEDED);
        when(paymentRepositoryPort.save(payment)).thenReturn(payment);

        Payment result = service.confirmPayment(payment, transaction);

        assertEquals(payment, result);
        verify(paymentRepositoryPort).save(payment);
        verify(paymentTransactionRepositoryPort).save(transaction);
    }

    @Test
    void failPaymentSavesPaymentAndTransaction() {
        Payment payment = payment();
        PaymentTransaction transaction = transaction(TransactionType.CHARGE, TransactionStatus.FAILED);
        when(paymentRepositoryPort.save(payment)).thenReturn(payment);

        Payment result = service.failPayment(payment, transaction);

        assertEquals(payment, result);
        verify(paymentRepositoryPort).save(payment);
        verify(paymentTransactionRepositoryPort).save(transaction);
    }

    @Test
    void cancelPaymentSavesPaymentAndTransaction() {
        Payment payment = payment();
        PaymentTransaction transaction = transaction(TransactionType.VOID, TransactionStatus.SUCCEEDED);
        when(paymentRepositoryPort.save(payment)).thenReturn(payment);

        Payment result = service.cancelPayment(payment, transaction);

        assertEquals(payment, result);
        verify(paymentRepositoryPort).save(payment);
        verify(paymentTransactionRepositoryPort).save(transaction);
    }

    private Payment payment() {
        return Payment.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "guest@example.com",
                "pi_test_123",
                new BigDecimal("150.00"),
                "USD",
                PaymentStatus.SUCCEEDED,
                "cus_123",
                "card",
                "4242",
                LocalDateTime.now(),
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    private PaymentTransaction transaction(TransactionType type, TransactionStatus status) {
        return PaymentTransaction.initiate(
                UUID.randomUUID(),
                type,
                new BigDecimal("150.00"),
                "USD",
                status,
                STRIPE_EVENT_ID,
                status == TransactionStatus.FAILED ? "card_declined" : null);
    }
}