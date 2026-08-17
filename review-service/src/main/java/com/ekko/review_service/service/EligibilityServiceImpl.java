package com.ekko.review_service.service;

import com.ekko.review_service.entity.EligibleReview;
import com.ekko.review_service.exception.NotEligibleToReviewException;
import com.ekko.review_service.messaging.dto.OrderConfirmedEvent;
import com.ekko.review_service.repository.EligibleReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EligibilityServiceImpl implements EligibilityService {

    private final EligibleReviewRepository eligibleReviewRepository;

    @Override
    public EligibleReview assertEligible(String customerId, UUID orderItemId, UUID orderId, UUID productId) {
        EligibleReview eligible = eligibleReviewRepository
                .findByOrderItemIdAndCustomerId(orderItemId, customerId)
                .orElseThrow(NotEligibleToReviewException::new);

        if (!eligible.getOrderId().equals(orderId) || !eligible.getProductId().equals(productId)) {
            throw new NotEligibleToReviewException();
        }

        return eligible;
    }

    @Override
    @Transactional
    public void registerEligibility(OrderConfirmedEvent event) {
        for (OrderConfirmedEvent.OrderItemConfirmed item : event.items()) {
            eligibleReviewRepository.insertEligibilityIfAbsent(
                    event.orderId(), item.orderItemId(), item.productId(), item.sellerKeycloakId(), event.customerId());
        }
    }
}