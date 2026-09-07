package com.ekko.review_service.validator;

import com.ekko.review_service.exception.ReviewEditWindowExpiredException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class ReviewEditWindowValidator {

    private static final int EDIT_WINDOW_DAYS = 15;

    public void assertWithinWindow(LocalDateTime createdAt) {
        Duration age = Duration.between(createdAt, LocalDateTime.now());
        if (age.toDays() > EDIT_WINDOW_DAYS) {
            throw new ReviewEditWindowExpiredException();
        }
    }
}
