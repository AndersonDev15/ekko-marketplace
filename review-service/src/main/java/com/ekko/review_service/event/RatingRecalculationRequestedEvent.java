package com.ekko.review_service.event;

import java.util.UUID;

public record RatingRecalculationRequestedEvent(UUID productId) {
}