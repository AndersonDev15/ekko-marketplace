package com.ekko.review_service.service;

import com.ekko.review_service.config.AbstractPostgresIntegrationTest;
import com.ekko.review_service.entity.EligibleReview;
import com.ekko.review_service.exception.NotEligibleToReviewException;
import com.ekko.review_service.messaging.dto.consume.OrderConfirmedEvent;
import com.ekko.review_service.repository.EligibleReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EligibilityServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final UUID CUSTOMER_KEYCLOAK_ID = UUID.randomUUID();
    private static final String CUSTOMER_ID = CUSTOMER_KEYCLOAK_ID.toString();

    @Autowired
    private EligibilityService eligibilityService;

    @Autowired
    private EligibleReviewRepository eligibleReviewRepository;

    @Test
    @DisplayName("registerEligibility persiste un registro por item del evento en la BD real")
    void registerEligibility_shouldPersistOneRowPerItem() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID item1 = UUID.randomUUID();
        UUID item2 = UUID.randomUUID();
        UUID product1 = UUID.randomUUID();
        UUID product2 = UUID.randomUUID();
        UUID seller1 = UUID.randomUUID();
        UUID seller2 = UUID.randomUUID();
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                orderId,
                "EKK-20250809-AB12",
                CUSTOMER_KEYCLOAK_ID,
                "guest@ekko.test",
                List.of(
                        new OrderConfirmedEvent.OrderItemConfirmed(
                                item1, product1, UUID.randomUUID(), 2, seller1, new BigDecimal("100.00")),
                        new OrderConfirmedEvent.OrderItemConfirmed(
                                item2, product2, UUID.randomUUID(), 1, seller2, new BigDecimal("50.00"))),
                LocalDateTime.of(2025, 8, 9, 13, 0));

        // when
        eligibilityService.registerEligibility(event);

        // then
        List<EligibleReview> eligible = eligibleReviewRepository.findEligibleWithoutReview(CUSTOMER_ID);
        assertThat(eligible).hasSize(2);
        assertThat(eligible)
                .extracting(EligibleReview::getOrderItemId)
                .containsExactlyInAnyOrder(item1, item2);
        assertThat(eligible)
                .extracting(EligibleReview::getSellerKeycloakId)
                .containsExactlyInAnyOrder(seller1, seller2);
    }

    @Test
    @DisplayName("registerEligibility es idempotente: repetir el mismo evento no duplica registros")
    void registerEligibility_shouldBeIdempotent() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID seller = UUID.randomUUID();
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                orderId,
                "EKK-20250809-AB12",
                CUSTOMER_KEYCLOAK_ID,
                "guest@ekko.test",
                List.of(new OrderConfirmedEvent.OrderItemConfirmed(
                        orderItemId, productId, UUID.randomUUID(), 1, seller, new BigDecimal("100.00"))),
                LocalDateTime.of(2025, 8, 9, 13, 0));

        // when
        eligibilityService.registerEligibility(event);
        eligibilityService.registerEligibility(event);

        // then
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM eligible_reviews WHERE customer_id = ?", Long.class, CUSTOMER_ID);
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("assertEligible no lanza cuando el registro existe y todos los campos coinciden")
    void assertEligible_shouldPassWhenRecordExistsAndAllFieldsMatch() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        insertEligibility(orderId, orderItemId, productId);

        // when
        // then
        assertThatCode(() -> eligibilityService.assertEligible(CUSTOMER_ID, orderItemId, orderId, productId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertEligible lanza NotEligibleToReviewException cuando no existe el registro")
    void assertEligible_shouldThrowWhenNoEligibilityRecordExists() {
        // given
        // no eligible rows for the customer

        // when
        // then
        assertThatThrownBy(() -> eligibilityService.assertEligible(
                CUSTOMER_ID, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(NotEligibleToReviewException.class);
    }

    @Test
    @DisplayName("assertEligible lanza NotEligibleToReviewException cuando el orderId del registro no coincide")
    void assertEligible_shouldThrowWhenOrderIdDoesNotMatch() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        insertEligibility(orderId, orderItemId, productId);

        // when
        // then
        assertThatThrownBy(() -> eligibilityService.assertEligible(
                CUSTOMER_ID, orderItemId, UUID.randomUUID(), productId))
                .isInstanceOf(NotEligibleToReviewException.class);
    }

    @Test
    @DisplayName("assertEligible lanza NotEligibleToReviewException cuando el productId del registro no coincide")
    void assertEligible_shouldThrowWhenProductIdDoesNotMatch() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        insertEligibility(orderId, orderItemId, productId);

        // when
        // then
        assertThatThrownBy(() -> eligibilityService.assertEligible(
                CUSTOMER_ID, orderItemId, orderId, UUID.randomUUID()))
                .isInstanceOf(NotEligibleToReviewException.class);
    }

    private void insertEligibility(UUID orderId, UUID orderItemId, UUID productId) {
        jdbcTemplate.update("""
                INSERT INTO eligible_reviews (order_id, order_item_id, product_id, seller_keycloak_id, customer_id)
                VALUES (?, ?, ?, ?, ?)
                """, orderId, orderItemId, productId, UUID.randomUUID(), CUSTOMER_ID);
    }
}