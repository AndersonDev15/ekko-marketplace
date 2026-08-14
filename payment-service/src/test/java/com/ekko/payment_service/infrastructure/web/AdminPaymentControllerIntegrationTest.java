package com.ekko.payment_service.infrastructure.web;

import com.ekko.payment_service.config.AbstractPostgresIntegrationTest;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.ekko.payment_service.util.JwtTestUtils.adminAuth;
import static com.ekko.payment_service.util.JwtTestUtils.customerAuth;
import static com.ekko.payment_service.util.TestConstants.CUSTOMER_KEYCLOAK_ID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AdminPaymentControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepositoryPort paymentRepositoryPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM payment_vendor_allocations");
        jdbcTemplate.execute("DELETE FROM payment_transactions");
        jdbcTemplate.execute("DELETE FROM payment_transfers");
        jdbcTemplate.execute("DELETE FROM payments");
    }

    @Test
    @DisplayName("GET /admin/payments/{paymentId} con JWT ADMIN -> 200 sin importar el dueno real")
    void adminGetPaymentByIdReturns200() throws Exception {
        Payment payment = saveCustomerPayment();

        mockMvc.perform(get("/admin/payments/{paymentId}", payment.getId())
                        .with(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payment.getId().toString()))
                .andExpect(jsonPath("$.customerId").value(CUSTOMER_KEYCLOAK_ID.toString()));
    }

    @Test
    @DisplayName("GET /admin/payments/by-order/{orderId} con JWT ADMIN -> 200")
    void adminGetPaymentByOrderIdReturns200() throws Exception {
        Payment payment = saveCustomerPayment();

        mockMvc.perform(get("/admin/payments/by-order/{orderId}", payment.getOrderId())
                        .with(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(payment.getOrderId().toString()));
    }

    @Test
    @DisplayName("GET /admin/payments/{paymentId}/transactions con JWT ADMIN -> 200 lista")
    void adminGetTransactionsReturns200() throws Exception {
        Payment payment = saveCustomerPayment();

        mockMvc.perform(get("/admin/payments/{paymentId}/transactions", payment.getId())
                        .with(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /admin/payments/{paymentId} con JWT customer (no ADMIN) -> 403 via @PreAuthorize")
    void adminGetPaymentByIdWithCustomerJwtReturns403() throws Exception {
        Payment payment = saveCustomerPayment();

        mockMvc.perform(get("/admin/payments/{paymentId}", payment.getId())
                        .with(customerAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /admin/payments/by-order/{orderId} con JWT customer -> 403")
    void adminGetPaymentByOrderIdWithCustomerJwtReturns403() throws Exception {
        Payment payment = saveCustomerPayment();

        mockMvc.perform(get("/admin/payments/by-order/{orderId}", payment.getOrderId())
                        .with(customerAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /admin/payments/{paymentId}/transactions con JWT customer -> 403")
    void adminGetTransactionsWithCustomerJwtReturns403() throws Exception {
        Payment payment = saveCustomerPayment();

        mockMvc.perform(get("/admin/payments/{paymentId}/transactions", payment.getId())
                        .with(customerAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /admin/payments/{paymentId} sin JWT -> 401 (ruta admin no permitAll)")
    void adminGetPaymentByIdWithoutJwtReturns401() throws Exception {
        Payment payment = saveCustomerPayment();

        mockMvc.perform(get("/admin/payments/{paymentId}", payment.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /admin/payments/by-order/{orderId} sin JWT -> 401")
    void adminGetPaymentByOrderIdWithoutJwtReturns401() throws Exception {
        Payment payment = saveCustomerPayment();

        mockMvc.perform(get("/admin/payments/by-order/{orderId}", payment.getOrderId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /admin/payments/{paymentId}/transactions sin JWT -> 401")
    void adminGetTransactionsWithoutJwtReturns401() throws Exception {
        Payment payment = saveCustomerPayment();

        mockMvc.perform(get("/admin/payments/{paymentId}/transactions", payment.getId()))
                .andExpect(status().isUnauthorized());
    }

    private Payment saveCustomerPayment() {
        Payment payment = Payment.initiate(
                UUID.randomUUID(),
                CUSTOMER_KEYCLOAK_ID,
                null,
                new BigDecimal("100.00"),
                "USD");
        payment.attachPaymentIntent("pi_admin_" + UUID.randomUUID(), "cus_test_123");
        payment.markSucceeded("card", "4242", LocalDateTime.now());
        return paymentRepositoryPort.save(payment);
    }
}