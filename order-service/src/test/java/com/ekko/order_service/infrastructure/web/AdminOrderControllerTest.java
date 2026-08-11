package com.ekko.order_service.infrastructure.web;

import com.ekko.order_service.application.mapper.OrderMapper;
import com.ekko.order_service.domain.exception.InvalidOrderStatusTransitionException;
import com.ekko.order_service.domain.model.Order;
import com.ekko.order_service.domain.model.OrderStatus;
import com.ekko.order_service.domain.port.in.CancelOrderUseCase;
import com.ekko.order_service.domain.port.in.GetAllOrdersUseCase;
import com.ekko.order_service.domain.port.in.GetOrderByOrderNumberForAdminUseCase;
import com.ekko.order_service.domain.port.in.UpdateOrderStatusUseCase;
import com.ekko.order_service.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.ekko.order_service.builder.OrderTestDataBuilder.anOrderBuilder;
import static com.ekko.order_service.util.JwtTestUtils.adminAuth;
import static com.ekko.order_service.util.JwtTestUtils.customerAuth;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminOrderController.class)
@Import({SecurityConfig.class, OrderMapper.class})
class AdminOrderControllerTest {

    private static final String ORDER_NUMBER = "EKK-20250809-AB12";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetAllOrdersUseCase getAllOrdersUseCase;

    @MockitoBean
    private GetOrderByOrderNumberForAdminUseCase getOrderByOrderNumberForAdminUseCase;

    @MockitoBean
    private CancelOrderUseCase cancelOrderUseCase;

    @MockitoBean
    private UpdateOrderStatusUseCase updateOrderStatusUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Nested
    @DisplayName("GET /admin/orders")
    class GetAll {

        @Test
        @DisplayName("sin JWT -> 401")
        void withoutJwtReturns401() throws Exception {
            mockMvc.perform(get("/admin/orders"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("JWT CUSTOMER -> 403")
        void customerReturns403() throws Exception {
            mockMvc.perform(get("/admin/orders").with(customerAuth()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("JWT ADMIN -> 200")
        void adminReturns200() throws Exception {
            when(getAllOrdersUseCase.execute(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(anOrderBuilder().build())));

            mockMvc.perform(get("/admin/orders").with(adminAuth()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("GET /admin/orders/{orderNumber}")
    class GetOne {

        @Test
        @DisplayName("ADMIN -> 200 sin validar ownership")
        void adminFetchesOrderWithoutOwnership() throws Exception {
            when(getOrderByOrderNumberForAdminUseCase.execute(ORDER_NUMBER))
                    .thenReturn(anOrderBuilder().build());

            mockMvc.perform(get("/admin/orders/{orderNumber}", ORDER_NUMBER).with(adminAuth()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderNumber").value(ORDER_NUMBER));

            verify(getOrderByOrderNumberForAdminUseCase).execute(ORDER_NUMBER);
        }
    }

    @Nested
    @DisplayName("PATCH /admin/orders/{orderNumber}/status")
    class UpdateStatus {

        @Test
        @DisplayName("transicion invalida -> 409")
        void invalidTransitionReturns409() throws Exception {
            when(updateOrderStatusUseCase.execute(eq(ORDER_NUMBER), eq(OrderStatus.PENDING)))
                    .thenThrow(new InvalidOrderStatusTransitionException(OrderStatus.CONFIRMED, OrderStatus.PENDING));

            mockMvc.perform(patch("/admin/orders/{orderNumber}/status", ORDER_NUMBER)
                            .with(adminAuth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "newStatus": "PENDING" }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("INVALID_STATUS_TRANSITION"));
        }

        @Test
        @DisplayName("transicion valida CONFIRMED->SHIPPED -> 200")
        void validTransitionReturns200() throws Exception {
            Order shipped = anOrderBuilder().status(OrderStatus.SHIPPED).build();
            when(updateOrderStatusUseCase.execute(eq(ORDER_NUMBER), eq(OrderStatus.SHIPPED)))
                    .thenReturn(shipped);

            mockMvc.perform(patch("/admin/orders/{orderNumber}/status", ORDER_NUMBER)
                            .with(adminAuth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "newStatus": "SHIPPED" }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SHIPPED"));
        }
    }
}