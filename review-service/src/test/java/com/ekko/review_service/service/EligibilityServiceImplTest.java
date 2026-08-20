package com.ekko.review_service.service;

import com.ekko.review_service.entity.EligibleReview;
import com.ekko.review_service.exception.NotEligibleToReviewException;
import com.ekko.review_service.messaging.dto.consume.OrderConfirmedEvent;
import com.ekko.review_service.repository.EligibleReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EligibilityServiceImplTest {

    private static final String CUSTOMER_ID = "customer-1";
    private static final UUID CUSTOMER_KEYCLOAK_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID ORDER_ITEM_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @Mock
    private EligibleReviewRepository eligibleReviewRepository;

    @InjectMocks
    private EligibilityServiceImpl eligibilityService;

    @Test
    @DisplayName("assertEligible no lanza cuando existe el registro y todos los campos coinciden")
    void assertEligible_shouldNotThrowWhenEligibilityRecordExistsAndAllFieldsMatch() {
        // given
        EligibleReview eligible = eligibleReview(ORDER_ID, ORDER_ITEM_ID, PRODUCT_ID);
        when(eligibleReviewRepository.findByOrderItemIdAndCustomerId(ORDER_ITEM_ID, CUSTOMER_ID))
                .thenReturn(Optional.of(eligible));

        // when
        // then
        assertThatCode(() -> eligibilityService.assertEligible(CUSTOMER_ID, ORDER_ITEM_ID, ORDER_ID, PRODUCT_ID))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("assertEligible lanza NotEligibleToReviewException cuando no existe registro de elegibilidad")
    void assertEligible_shouldThrowNotEligibleToReviewExceptionWhenNoEligibilityRecordExists() {
        // given
        when(eligibleReviewRepository.findByOrderItemIdAndCustomerId(ORDER_ITEM_ID, CUSTOMER_ID))
                .thenReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> eligibilityService.assertEligible(CUSTOMER_ID, ORDER_ITEM_ID, ORDER_ID, PRODUCT_ID))
                .isInstanceOf(NotEligibleToReviewException.class);
    }

    @Test
    @DisplayName("assertEligible lanza la misma excepción cuando el orderId no coincide")
    void assertEligible_shouldThrowNotEligibleToReviewExceptionWhenOrderIdDoesNotMatch() {
        // given
        EligibleReview eligible = eligibleReview(ORDER_ID, ORDER_ITEM_ID, PRODUCT_ID);
        when(eligibleReviewRepository.findByOrderItemIdAndCustomerId(ORDER_ITEM_ID, CUSTOMER_ID))
                .thenReturn(Optional.of(eligible));

        // when
        // then
        assertThatThrownBy(() -> eligibilityService.assertEligible(CUSTOMER_ID, ORDER_ITEM_ID, UUID.randomUUID(), PRODUCT_ID))
                .isInstanceOf(NotEligibleToReviewException.class);
    }

    @Test
    @DisplayName("assertEligible lanza la misma excepción cuando el productId no coincide")
    void assertEligible_shouldThrowNotEligibleToReviewExceptionWhenProductIdDoesNotMatch() {
        // given
        EligibleReview eligible = eligibleReview(ORDER_ID, ORDER_ITEM_ID, PRODUCT_ID);
        when(eligibleReviewRepository.findByOrderItemIdAndCustomerId(ORDER_ITEM_ID, CUSTOMER_ID))
                .thenReturn(Optional.of(eligible));

        // when
        // then
        assertThatThrownBy(() -> eligibilityService.assertEligible(CUSTOMER_ID, ORDER_ITEM_ID, ORDER_ID, UUID.randomUUID()))
                .isInstanceOf(NotEligibleToReviewException.class);
    }

    @Test
    @DisplayName("registerEligibility inserta una vez por cada item del evento")
    void registerEligibility_shouldInsertForEachItemOfTheEvent() {
        // given
        UUID item1 = UUID.randomUUID();
        UUID item2 = UUID.randomUUID();
        UUID product1 = UUID.randomUUID();
        UUID product2 = UUID.randomUUID();
        UUID seller1 = UUID.randomUUID();
        UUID seller2 = UUID.randomUUID();
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                ORDER_ID,
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
        verify(eligibleReviewRepository).insertEligibilityIfAbsent(
                ORDER_ID, item1, product1, seller1, CUSTOMER_KEYCLOAK_ID.toString());
        verify(eligibleReviewRepository).insertEligibilityIfAbsent(
                ORDER_ID, item2, product2, seller2, CUSTOMER_KEYCLOAK_ID.toString());
    }

    @Test
    @DisplayName("registerEligibility no inserta nada cuando el evento no trae items")
    void registerEligibility_shouldNotInsertAnythingWhenEventHasNoItems() {
        // given
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                ORDER_ID, "EKK-20250809-AB12", CUSTOMER_KEYCLOAK_ID, "guest@ekko.test", List.of(),
                LocalDateTime.of(2025, 8, 9, 13, 0));

        // when
        eligibilityService.registerEligibility(event);

        // then
        verify(eligibleReviewRepository, never()).insertEligibilityIfAbsent(any(), any(), any(), any(), any());
        verify(eligibleReviewRepository, never()).insertEligibilityIfAbsent(eq(ORDER_ID), any(), any(), any(), eq(CUSTOMER_ID));
    }

    private EligibleReview eligibleReview(UUID orderId, UUID orderItemId, UUID productId) {
        return EligibleReview.builder()
                .orderId(orderId)
                .orderItemId(orderItemId)
                .productId(productId)
                .customerId(CUSTOMER_ID)
                .build();
    }
}