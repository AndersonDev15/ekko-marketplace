package com.ekko.payment_service.infrastructure.web;

import com.ekko.payment_service.domain.exception.RefundAmountExceededException;
import com.ekko.payment_service.domain.exception.RefundNotAllowedException;
import com.ekko.payment_service.domain.command.CreateRefundCommand;
import com.ekko.payment_service.domain.enums.PaymentStatus;
import com.ekko.payment_service.domain.model.Refund;
import com.ekko.payment_service.domain.enums.RefundReason;
import com.ekko.payment_service.domain.port.in.CreateRefundUseCase;
import com.ekko.payment_service.infrastructure.config.SecurityConfig;
import com.ekko.payment_service.infrastructure.persistence.adapter.in.web.RefundController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static com.ekko.payment_service.util.JwtTestUtils.adminAuth;
import static com.ekko.payment_service.util.JwtTestUtils.customerAuth;
import static com.ekko.payment_service.util.TestConstants.ADMIN_KEYCLOAK_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RefundController.class)
@Import({SecurityConfig.class})
class RefundControllerTest {

    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final String STRIPE_REFUND_ID = "re_test_123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @MockitoBean
    private CreateRefundUseCase createRefundUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("con JWT ADMIN -> 201, requestedBy viene del JWT (claim sub) no del body")
    void createRefundWithAdminJwtReturns201AndRequestedByFromJwt() throws Exception {
        when(createRefundUseCase.execute(any(CreateRefundCommand.class))).thenReturn(aRefund());

        mockMvc.perform(post("/admin/payments/{paymentId}/refunds", PAYMENT_ID)
                        .with(adminAuth(jwtAuthenticationConverter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 40.00, "reason": "DUPLICATE", "notes": "admin note" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.stripeRefundId").value(STRIPE_REFUND_ID))
                .andExpect(jsonPath("$.amount").value(40.00))
                .andExpect(jsonPath("$.reason").value("DUPLICATE"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        ArgumentCaptor<CreateRefundCommand> captor = ArgumentCaptor.forClass(CreateRefundCommand.class);
        verify(createRefundUseCase).execute(captor.capture());
        assertEquals(PAYMENT_ID, captor.getValue().paymentId());
        assertEquals(0, new BigDecimal("40.00").compareTo(captor.getValue().amount()));
        assertEquals(RefundReason.DUPLICATE, captor.getValue().reason());
        assertEquals("admin note", captor.getValue().notes());
        assertEquals(ADMIN_KEYCLOAK_ID, captor.getValue().requestedBy());
    }

    @Test
    @DisplayName("sin JWT -> 401 (ruta admin no permitAll, anyRequest().authenticated())")
    void createRefundWithoutJwtReturns401() throws Exception {
        mockMvc.perform(post("/admin/payments/{paymentId}/refunds", PAYMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 40.00, "reason": "DUPLICATE" }
                                """))
                .andExpect(status().isUnauthorized());

        verify(createRefundUseCase, never()).execute(any(CreateRefundCommand.class));
    }

    @Test
    @DisplayName("con JWT no-ADMIN (customer) -> 403 via @PreAuthorize")
    void createRefundWithCustomerJwtReturns403() throws Exception {
        mockMvc.perform(post("/admin/payments/{paymentId}/refunds", PAYMENT_ID)
                        .with(customerAuth(jwtAuthenticationConverter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 40.00, "reason": "DUPLICATE" }
                                """))
                .andExpect(status().isForbidden());

        verify(createRefundUseCase, never()).execute(any(CreateRefundCommand.class));
    }

    @Test
    @DisplayName("con JWT invalido -> 401 via resource server")
    void createRefundWithInvalidTokenReturns401() throws Exception {
        when(jwtDecoder.decode(any())).thenThrow(new BadJwtException("invalid token"));

        mockMvc.perform(post("/admin/payments/{paymentId}/refunds", PAYMENT_ID)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 40.00, "reason": "DUPLICATE" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("payment no SUCCEEDED -> 409 (RefundNotAllowedException)")
    void createRefundOnNonSucceededPaymentReturns409() throws Exception {
        when(createRefundUseCase.execute(any(CreateRefundCommand.class)))
                .thenThrow(new RefundNotAllowedException(PAYMENT_ID, PaymentStatus.PENDING));

        mockMvc.perform(post("/admin/payments/{paymentId}/refunds", PAYMENT_ID)
                        .with(adminAuth(jwtAuthenticationConverter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 40.00, "reason": "DUPLICATE" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REFUND_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("monto que excede el disponible -> 409 (RefundAmountExceededException)")
    void createRefundAmountExceededReturns409() throws Exception {
        when(createRefundUseCase.execute(any(CreateRefundCommand.class)))
                .thenThrow(new RefundAmountExceededException(
                        PAYMENT_ID, new BigDecimal("150.00"), BigDecimal.ZERO, new BigDecimal("100.00")));

        mockMvc.perform(post("/admin/payments/{paymentId}/refunds", PAYMENT_ID)
                        .with(adminAuth(jwtAuthenticationConverter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 150.00, "reason": "DUPLICATE" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REFUND_AMOUNT_EXCEEDED"));
    }

    @Test
    @DisplayName("body invalido (amount nulo / negativo, reason nulo) -> 400 por Bean Validation")
    void createRefundWithInvalidBodyReturns400() throws Exception {
        mockMvc.perform(post("/admin/payments/{paymentId}/refunds", PAYMENT_ID)
                        .with(adminAuth(jwtAuthenticationConverter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": -5.00, "reason": null }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        verify(createRefundUseCase, never()).execute(any(CreateRefundCommand.class));
    }

    private Refund aRefund() {
        Refund refund = Refund.initiate(
                PAYMENT_ID,
                new BigDecimal("40.00"),
                RefundReason.DUPLICATE,
                ADMIN_KEYCLOAK_ID,
                "admin note");
        refund.attachStripeRefundId(STRIPE_REFUND_ID);
        return refund;
    }
}