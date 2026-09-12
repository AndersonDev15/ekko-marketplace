package com.ekko.order_service.infrastructure.web;

import com.ekko.order_service.application.mapper.OrderMapperImpl;
import com.ekko.order_service.application.exception.OrderAccessDeniedException;
import com.ekko.order_service.domain.exception.OrderCancellationNotAllowedException;
import com.ekko.order_service.domain.exception.InvalidGuestEmailException;
import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.model.OrderDraft;
import com.ekko.order_service.domain.enums.OrderStatus;
import com.ekko.order_service.domain.port.in.CancelOrderUseCase;
import com.ekko.order_service.domain.port.in.CreateOrderUseCase;
import com.ekko.order_service.domain.port.in.GetMyOrdersUseCase;
import com.ekko.order_service.domain.port.in.GetOrderByOrderNumberUseCase;
import com.ekko.order_service.infrastructure.config.SecurityConfig;
import com.ekko.order_service.domain.port.out.ProductServicePort;
import com.ekko.order_service.infrastructure.persistence.adapter.in.web.controller.OrderController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.ekko.order_service.builder.OrderTestDataBuilder.anOrder;
import static com.ekko.order_service.util.JwtTestUtils.customerAuth;
import static com.ekko.order_service.util.JwtTestUtils.otherCustomerAuth;
import static com.ekko.order_service.util.TestConstants.CUSTOMER_KEYCLOAK_ID;
import static com.ekko.order_service.util.TestConstants.GUEST_EMAIL;
import static com.ekko.order_service.util.TestConstants.VARIANT_ID;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, OrderMapperImpl.class})
class OrderControllerTest {

    private static final String ORDER_NUMBER = "EKK-20250809-AB12";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @MockitoBean
    private CreateOrderUseCase createOrderUseCase;

    @MockitoBean
    private GetOrderByOrderNumberUseCase getOrderByOrderNumberUseCase;

    @MockitoBean
    private GetMyOrdersUseCase getMyOrdersUseCase;

    @MockitoBean
    private CancelOrderUseCase cancelOrderUseCase;

    @MockitoBean
    private ProductServicePort productServicePort;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Nested
    @DisplayName("POST /orders")
    class CreateOrder {

        @Test
        @DisplayName("sin JWT y guestEmail valido -> 201 con customerId nulo")
        void createWithoutJwtUsesNullCustomerId() throws Exception {
            when(createOrderUseCase.execute(any(OrderDraft.class))).thenReturn(anOrder());

            mockMvc.perform(post("/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(guestBody(GUEST_EMAIL)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.orderNumber").value(ORDER_NUMBER))
                    .andExpect(jsonPath("$.status").value("PENDING"));

            ArgumentCaptor<OrderDraft> captor = ArgumentCaptor.forClass(OrderDraft.class);
            verify(createOrderUseCase).execute(captor.capture());
            assertNull(captor.getValue().customerId());
            assertEquals(GUEST_EMAIL, captor.getValue().guestEmail());
            assertEquals(GUEST_EMAIL, captor.getValue().customerEmail());
        }

        @Test
        @DisplayName("con JWT de customer -> 201 con customerId del subject, guestEmail nulo y email del claim")
        void createWithJwtUsesSubjectAsCustomerId() throws Exception {
            when(createOrderUseCase.execute(any(OrderDraft.class))).thenReturn(anOrder());

            mockMvc.perform(post("/orders")
                            .with(customerAuth(jwtAuthenticationConverter))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(guestBody(GUEST_EMAIL)))
                    .andExpect(status().isCreated());

            ArgumentCaptor<OrderDraft> captor = ArgumentCaptor.forClass(OrderDraft.class);
            verify(createOrderUseCase).execute(captor.capture());
            assertEquals(CUSTOMER_KEYCLOAK_ID, captor.getValue().customerId());
            assertNull(captor.getValue().guestEmail());
            assertEquals("customer@example.com", captor.getValue().customerEmail());
        }

        @Test
        @DisplayName("sin JWT y sin guestEmail -> 400 (InvalidGuestEmailException -> GlobalExceptionHandler)")
        void createWithoutGuestEmailPropagatesInvalidGuestEmail() throws Exception {
            when(createOrderUseCase.execute(any(OrderDraft.class)))
                    .thenThrow(new InvalidGuestEmailException("Guest email is required"));

            mockMvc.perform(post("/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(guestBody(null)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INVALID_GUEST_EMAIL"));
        }

        @Test
        @DisplayName("body invalido (shippingAddress nulo) -> 400 por Bean Validation")
        void createWithInvalidBodyReturns400() throws Exception {
            mockMvc.perform(post("/orders")
                            .with(customerAuth(jwtAuthenticationConverter))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "items": [
                                        { "variantId": "%s", "quantity": 1, "unitPrice": 100.00 }
                                      ]
                                    }
                                    """.formatted(VARIANT_ID)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

            verify(createOrderUseCase, never()).execute(any(OrderDraft.class));
        }
    }

    @Nested
    @DisplayName("GET /orders/{orderNumber}")
    class GetOrder {

        @Test
        @DisplayName("sin JWT con guestEmail query param -> 200")
        void getByOrderNumberAsGuestReturns200() throws Exception {
            when(getOrderByOrderNumberUseCase.execute(eq(ORDER_NUMBER), any(), eq(GUEST_EMAIL)))
                    .thenReturn(anOrder());

            mockMvc.perform(get("/orders/{orderNumber}", ORDER_NUMBER)
                            .queryParam("guestEmail", GUEST_EMAIL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderNumber").value(ORDER_NUMBER))
                    .andExpect(jsonPath("$.items", hasSize(1)));

            verify(getOrderByOrderNumberUseCase).execute(eq(ORDER_NUMBER), any(), eq(GUEST_EMAIL));
        }

        @Test
        @DisplayName("con JWT de customer no dueno -> 403 (OrderAccessDeniedException)")
        void getByOrderNumberAsNonOwnerReturns403() throws Exception {
            when(getOrderByOrderNumberUseCase.execute(eq(ORDER_NUMBER), any(), any()))
                    .thenThrow(new OrderAccessDeniedException());

            mockMvc.perform(get("/orders/{orderNumber}", ORDER_NUMBER)
                            .with(otherCustomerAuth(jwtAuthenticationConverter)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("ORDER_ACCESS_DENIED"));
        }

        @Test
        @DisplayName("con JWT invalido/expirado -> 401 via resource server")
        void getByOrderNumberWithInvalidTokenReturns401() throws Exception {
            when(jwtDecoder.decode(any())).thenThrow(new BadJwtException("invalid token"));

            mockMvc.perform(get("/orders/{orderNumber}", ORDER_NUMBER)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /orders/me")
    class GetMyOrders {

        @Test
        @DisplayName("sin JWT -> 401 (ruta protegida por @PreAuthorize CUSTOMER)")
        void getMyOrdersWithoutJwtReturns401() throws Exception {
            mockMvc.perform(get("/orders/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("con JWT de customer -> 200 y pagina pasada correctamente")
        void getMyOrdersWithJwtReturnsPage() throws Exception {
            org.springframework.data.domain.Page<Order> page =
                    new org.springframework.data.domain.PageImpl<>(List.of(anOrder()));
            when(getMyOrdersUseCase.execute(eq(CUSTOMER_KEYCLOAK_ID), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/orders/me")
                            .with(customerAuth(jwtAuthenticationConverter))
                            .queryParam("page", "1")
                            .queryParam("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].orderNumber").value(ORDER_NUMBER));

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(getMyOrdersUseCase).execute(eq(CUSTOMER_KEYCLOAK_ID), pageableCaptor.capture());
            assertEquals(1, pageableCaptor.getValue().getPageNumber());
            assertEquals(5, pageableCaptor.getValue().getPageSize());
        }
    }

    @Nested
    @DisplayName("POST /orders/{orderNumber}/cancel")
    class CancelOrder {

        @Test
        @DisplayName("guest cancelable -> 200 con guestEmail del body")
        void cancelAsGuestReturns200() throws Exception {
            when(cancelOrderUseCase.execute(eq(ORDER_NUMBER), any(), eq(GUEST_EMAIL), eq(false)))
                    .thenReturn(anOrder());

            mockMvc.perform(post("/orders/{orderNumber}/cancel", ORDER_NUMBER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "guestEmail": "%s" }
                                    """.formatted(GUEST_EMAIL)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderNumber").value(ORDER_NUMBER));

            verify(cancelOrderUseCase).execute(eq(ORDER_NUMBER), any(), eq(GUEST_EMAIL), eq(false));
        }

        @Test
        @DisplayName("orden no cancelable -> 409 (OrderCancellationNotAllowedException)")
        void cancelNonCancellableReturns409() throws Exception {
            when(cancelOrderUseCase.execute(eq(ORDER_NUMBER), any(), eq(GUEST_EMAIL), eq(false)))
                    .thenThrow(new OrderCancellationNotAllowedException(OrderStatus.SHIPPED));

            mockMvc.perform(post("/orders/{orderNumber}/cancel", ORDER_NUMBER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "guestEmail": "%s" }
                                    """.formatted(GUEST_EMAIL)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("CANCELLATION_NOT_ALLOWED"));
        }
    }

    private String guestBody(String guestEmail) {
        return """
                {
                  "guestEmail": %s,
                  "shippingAddress": {
                    "fullName": "John Doe",
                    "phone": "+5491112345678",
                    "addressLine": "Av. Siempre Viva 742",
                    "city": "Buenos Aires",
                    "state": "Buenos Aires",
                    "country": "AR",
                    "postalCode": "1414"
                  },
                  "items": [
                    { "variantId": "%s", "quantity": 2, "unitPrice": 100.00 }
                  ],
                  "notes": "fragile"
                }
                """.formatted(guestEmail == null ? "null" : "\"" + guestEmail + "\"", VARIANT_ID);
    }
}