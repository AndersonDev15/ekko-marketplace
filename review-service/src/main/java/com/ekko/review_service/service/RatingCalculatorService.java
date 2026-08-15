package com.ekko.review_service.service;

import java.util.UUID;

public interface RatingCalculatorService {

    void recalculate(UUID productId);
}