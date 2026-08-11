package com.ekko.order_service.infrastructure.web;

import com.ekko.order_service.builder.OrderTestDataBuilder;
import com.ekko.order_service.config.AbstractPostgresIntegrationTest;
import com.ekko.order_service.domain.model.ProductVariant;
import com.ekko.order_service.domain.port.out.OrderEventPublisherPort;
import com.ekko.order_service.domain.port.out.ProductServicePort;
import com.ekko.order_service.infrastructure.persistence.entity.OrderEntity;
import com.ekko.order_service.infrastructure.persistence.repository.OrderJpaRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.ekko.order_service.util.JwtTestUtils.customerAuth;
import static com.ekko.order_service.util.TestConstants.ADDRESS_CITY;
import static com.ekko.order_service.util.TestConstants.ADDRESS_COUNTRY;
import static com.ekko.order_service.util.TestConstants.ADDRESS_FULL_NAME;
import static com.ekko.order_service.util.TestConstants.ADDRESS_LINE;
import static com.ekko.order_service.util.TestConstants.ADDRESS_PHONE;
import static com.ekko.order_service.util.TestConstants.ADDRESS_POSTAL_CODE;
import static com.ekko.order_service.util.TestConstants.ADDRESS_STATE;
import static com.ekko.order_service.util.TestConstants.CUSTOMER_KEYCLOAK_ID;
import static com.ekko.order_service.util.TestConstants.GUEST_EMAIL;
import static com.ekko.order_service.util.TestConstants.VARIANT_ID;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@AutoConfigureMockMvc
class OrderControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @MockitoBean
    private ProductServicePort productServicePort;

    @MockitoBean
    private OrderEventPublisherPort orderEventPublisherPort;

    @BeforeEach
    void setUp() {
        whenVariantAvailable();
    }

    @Nested
    @DisplayName("Flujo guest")
    class GuestFlow {

        @Test
        @DisplayName("POST /orders guest -> GET /orders/{orderNumber} recupera datos persistidos")
        void guestCreatesThenFetchesOrder() throws Exception {
            String orderNumber = createGuestOrder();

            mockMvc.perform(get("/orders/{orderNumber}", orderNumber)
                            .queryParam("guestEmail", GUEST_EMAIL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderNumber").value(orderNumber))
                    .andExpect(jsonPath("$.guestEmail").value(GUEST_EMAIL))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.total").value(200.00))
                    .andExpect(jsonPath("$.items", hasSize(1)))
                    .andExpect(jsonPath("$.items[0].variantId").value(VARIANT_ID.toString()))
                    .andExpect(jsonPath("$.items[0].quantity").value(2))
                    .andExpect(jsonPath("$.address.fullName").value(ADDRESS_FULL_NAME))
                    .andExpect(jsonPath("$.address.city").value(ADDRESS_CITY))
                    .andExpect(jsonPath("$.address.country").value(ADDRESS_COUNTRY));

            OrderEntity persisted = orderJpaRepository.findByOrderNumber(orderNumber).orElseThrow();
            assertEquals(1, persisted.getItems().size());
            assertEquals(GUEST_EMAIL, persisted.getGuestEmail());
            assertNotNull(persisted.getAddress());
            assertEquals(ADDRESS_LINE, persisted.getAddress().getAddressLine());
        }

        @Test
        @DisplayName("GET /orders/{orderNumber} con guestEmail incorrecto -> 403")
        void guestWithWrongEmailIsForbidden() throws Exception {
            String orderNumber = createGuestOrder();

            mockMvc.perform(get("/orders/{orderNumber}", orderNumber)
                            .queryParam("guestEmail", "otro@example.com"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Flujo customer autenticado")
    class CustomerFlow {

        @Test
        @DisplayName("POST /orders con JWT -> GET /orders/me lista la orden creada")
        void customerCreatesThenSeesInMyOrders() throws Exception {
            String orderNumber = createCustomerOrder();

            mockMvc.perform(get("/orders/me").with(customerAuth()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[?(@.orderNumber == '%s')]"
                            .formatted(orderNumber), hasSize(1)));

            OrderEntity persisted = orderJpaRepository.findByOrderNumber(orderNumber).orElseThrow();
            assertEquals(CUSTOMER_KEYCLOAK_ID, persisted.getCustomerId());
        }
    }

    private String createGuestOrder() throws Exception {
        return createOrder(GUEST_EMAIL, null);
    }

    private String createCustomerOrder() throws Exception {
        return createOrder(null, customerAuth());
    }

    private String createOrder(String guestEmail, RequestPostProcessor auth) throws Exception {
        MockHttpServletRequestBuilder builder = post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(guestEmail));
        if (auth != null) {
            builder.with(auth);
        }

        MvcResult result = mockMvc.perform(builder)
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("orderNumber").asText();
    }

    private String orderBody(String guestEmail) {
        String emailField = guestEmail == null ? "null" : "\"" + guestEmail + "\"";
        return """
                {
                  "guestEmail": %s,
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
                    { "variantId": "%s", "quantity": 2, "unitPrice": 100.00 }
                  ],
                  "notes": "fragile"
                }
                """.formatted(
                emailField,
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