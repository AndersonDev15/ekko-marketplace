package com.ekko.order_service.infrastructure.web;

import com.ekko.order_service.builder.OrderTestDataBuilder;
import com.ekko.order_service.config.AbstractPostgresIntegrationTest;
import com.ekko.order_service.domain.model.OrderStatus;
import com.ekko.order_service.domain.model.ProductVariant;
import com.ekko.order_service.domain.port.out.OrderEventPublisherPort;
import com.ekko.order_service.domain.port.out.ProductServicePort;
import com.ekko.order_service.infrastructure.persistence.entity.ChangedByType;
import com.ekko.order_service.infrastructure.persistence.entity.OrderEntity;
import com.ekko.order_service.infrastructure.persistence.entity.OrderStatusHistoryEntity;
import com.ekko.order_service.infrastructure.persistence.repository.OrderJpaRepository;
import com.ekko.order_service.infrastructure.persistence.repository.OrderStatusHistoryJpaRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.ekko.order_service.util.JwtTestUtils.adminAuth;
import static com.ekko.order_service.util.JwtTestUtils.customerAuth;
import static com.ekko.order_service.util.TestConstants.ADDRESS_CITY;
import static com.ekko.order_service.util.TestConstants.ADDRESS_COUNTRY;
import static com.ekko.order_service.util.TestConstants.ADDRESS_FULL_NAME;
import static com.ekko.order_service.util.TestConstants.ADDRESS_LINE;
import static com.ekko.order_service.util.TestConstants.ADDRESS_PHONE;
import static com.ekko.order_service.util.TestConstants.ADDRESS_POSTAL_CODE;
import static com.ekko.order_service.util.TestConstants.ADDRESS_STATE;
import static com.ekko.order_service.util.TestConstants.VARIANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@AutoConfigureMockMvc
class AdminOrderControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private OrderStatusHistoryJpaRepository orderStatusHistoryJpaRepository;

    @MockitoBean
    private ProductServicePort productServicePort;

    @MockitoBean
    private OrderEventPublisherPort orderEventPublisherPort;

    @BeforeEach
    void setUp() {
        whenVariantAvailable();
    }

    @Test
    @DisplayName("PATCH CONFIRMED->SHIPPED persistido + history con changedByType ADMIN")
    void adminTransitionsOrderToShipped() throws Exception {
        String orderNumber = createCustomerOrder();
        forceConfirmed(orderNumber);

        mockMvc.perform(patch("/admin/orders/{orderNumber}/status", orderNumber)
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "newStatus": "SHIPPED" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(orderNumber))
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        OrderEntity persisted = orderJpaRepository.findByOrderNumber(orderNumber).orElseThrow();
        assertEquals(OrderStatus.SHIPPED, persisted.getStatus());

        List<OrderStatusHistoryEntity> history =
                orderStatusHistoryJpaRepository.findAll().stream()
                        .filter(h -> h.getOrder().getId().equals(persisted.getId()))
                        .toList();
        assertEquals(1, history.stream()
                .filter(h -> h.getChangedByType() == ChangedByType.ADMIN
                        && h.getStatus() == OrderStatus.SHIPPED)
                .count());
    }

    @Test
    @DisplayName("PATCH transicion invalida desde CONFIRMED -> 409 y no cambia estado")
    void adminInvalidTransitionReturns409() throws Exception {
        String orderNumber = createCustomerOrder();
        forceConfirmed(orderNumber);

        mockMvc.perform(patch("/admin/orders/{orderNumber}/status", orderNumber)
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "newStatus": "PENDING" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_STATUS_TRANSITION"));

        OrderEntity persisted = orderJpaRepository.findByOrderNumber(orderNumber).orElseThrow();
        assertEquals(OrderStatus.CONFIRMED, persisted.getStatus());
    }

    private String createCustomerOrder() throws Exception {
        MockHttpServletRequestBuilder builder = post("/orders")
                .with(customerAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderBody());

        MvcResult result = mockMvc.perform(builder)
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("orderNumber").asText();
    }

    private void forceConfirmed(String orderNumber) {
        OrderEntity entity = orderJpaRepository.findByOrderNumber(orderNumber).orElseThrow();
        entity.setStatus(OrderStatus.CONFIRMED);
        entity.setUpdatedAt(LocalDateTime.now());
        orderJpaRepository.saveAndFlush(entity);
    }

    private String orderBody() {
        return """
                {
                  "shippingAddress": {
                    "fullName": "%s",
                    "phone": "%s",
                    "addressLine": "%s",
                    "city": "%s",
                    "state": "%s",
                    "country": "%s",
                    "postalCode": "%s"
                  },
                  "items": [
                    { "variantId": "%s", "quantity": 1, "unitPrice": 100.00 }
                  ],
                  "notes": "fragile"
                }
                """.formatted(
                ADDRESS_FULL_NAME, ADDRESS_PHONE, ADDRESS_LINE,
                ADDRESS_CITY, ADDRESS_STATE, ADDRESS_COUNTRY, ADDRESS_POSTAL_CODE,
                VARIANT_ID);
    }

    private void whenVariantAvailable() {
        ProductVariant variant = OrderTestDataBuilder.aProductVariant(10L);
        org.mockito.BDDMockito.given(productServicePort.getVariantsInfo(anyList()))
                .willReturn(List.of(variant));
    }
}