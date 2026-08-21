package com.ekko.payment_service.infrastructure.web;

import com.ekko.payment_service.config.AbstractPostgresIntegrationTest;
import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.port.out.PaymentGatewayPort;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.util.JwtTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static com.ekko.payment_service.util.JwtTestUtils.adminAuth;
import static com.ekko.payment_service.util.JwtTestUtils.customerAuth;
import static com.ekko.payment_service.util.TestConstants.ADMIN_KEYCLOAK_ID;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class RefundControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String PAYMENT_INTENT_ID = "pi_test_123";
    private static final String STRIPE_REFUND_ID = "re_test_123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Autowired
    private PaymentRepositoryPort paymentRepositoryPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private PaymentGatewayPort paymentGatewayPort;

    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM refunds");
        jdbcTemplate.execute("DELETE FROM payment_transactions");
        jdbcTemplate.execute("DELETE FROM payments");
    }

    @Test
    @DisplayName("POST con JWT ADMIN sobre payment SUCCEEDED -> 201 y refund persistido con requested_by del JWT")
    void createRefundHappyPathReturns201AndPersistsRequestedBy() throws Exception {
        Payment payment = saveSucceededPayment(new BigDecimal("100.00"));
        when(paymentGatewayPort.createRefund(eq(PAYMENT_INTENT_ID), any(BigDecimal.class), any()))
                .thenReturn(STRIPE_REFUND_ID);

        mockMvc.perform(post("/admin/payments/{paymentId}/refunds", payment.getId())
                        .with(adminAuth(jwtAuthenticationConverter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 40.00, "reason": "DUPLICATE", "notes": "admin note" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(payment.getId().toString()))
                .andExpect(jsonPath("$.stripeRefundId").value(STRIPE_REFUND_ID))
                .andExpect(jsonPath("$.amount").value(40.00))
                .andExpect(jsonPath("$.reason").value("DUPLICATE"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt", notNullValue()));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT requested_by, notes, amount FROM refunds WHERE payment_id = ?", payment.getId());
        assertEquals(ADMIN_KEYCLOAK_ID, row.get("requested_by"));
        assertEquals("admin note", row.get("notes"));
    }

    @Test
    @DisplayName("POST sin JWT -> 401 (ruta admin no permitAll)")
    void createRefundWithoutJwtReturns401() throws Exception {
        mockMvc.perform(post("/admin/payments/{paymentId}/refunds", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 40.00, "reason": "DUPLICATE" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST con JWT customer (no ADMIN) -> 403 via @PreAuthorize")
    void createRefundWithCustomerJwtReturns403() throws Exception {
        mockMvc.perform(post("/admin/payments/{paymentId}/refunds", UUID.randomUUID())
                        .with(customerAuth(jwtAuthenticationConverter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 40.00, "reason": "DUPLICATE" }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST sobre payment no SUCCEEDED -> 409 REFUND_NOT_ALLOWED")
    void createRefundOnNonSucceededPaymentReturns409() throws Exception {
        Payment payment = savePendingPayment(new BigDecimal("100.00"));

        mockMvc.perform(post("/admin/payments/{paymentId}/refunds", payment.getId())
                        .with(adminAuth(jwtAuthenticationConverter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 40.00, "reason": "DUPLICATE" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REFUND_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("POST con monto que excede el disponible -> 409 REFUND_AMOUNT_EXCEEDED")
    void createRefundAmountExceededReturns409() throws Exception {
        Payment payment = saveSucceededPayment(new BigDecimal("100.00"));
        when(paymentGatewayPort.createRefund(eq(PAYMENT_INTENT_ID), any(BigDecimal.class), any()))
                .thenReturn(STRIPE_REFUND_ID);

        mockMvc.perform(post("/admin/payments/{paymentId}/refunds", payment.getId())
                        .with(adminAuth(jwtAuthenticationConverter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 150.00, "reason": "DUPLICATE" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REFUND_AMOUNT_EXCEEDED"));
    }

    private Payment saveSucceededPayment(BigDecimal amount) {
        Payment payment = Payment.initiate(UUID.randomUUID(), null, "guest@example.com", amount, "USD");
        payment.attachPaymentIntent(PAYMENT_INTENT_ID, "cus_test_123");
        payment.markSucceeded("card", "4242", LocalDateTime.now());
        return paymentRepositoryPort.save(payment);
    }

    private Payment savePendingPayment(BigDecimal amount) {
        Payment payment = Payment.initiate(UUID.randomUUID(), null, "guest@example.com", amount, "USD");
        payment.attachPaymentIntent(PAYMENT_INTENT_ID, "cus_test_123");
        return paymentRepositoryPort.save(payment);
    }
}