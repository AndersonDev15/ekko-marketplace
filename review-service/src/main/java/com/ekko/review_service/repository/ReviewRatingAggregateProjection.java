package com.ekko.review_service.repository;

import java.math.BigDecimal;

public interface ReviewRatingAggregateProjection {

    BigDecimal getAverageRating();

    Long getReviewCount();
}