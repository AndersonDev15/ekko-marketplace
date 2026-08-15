package com.ekko.review_service.specification;

import com.ekko.review_service.entity.Review;
import com.ekko.review_service.enums.ReviewStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class ReviewSpecifications {

    private ReviewSpecifications() {
    }

    public static Specification<Review> hasStatus(ReviewStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Review> hasProductId(UUID productId) {
        return (root, query, cb) -> cb.equal(root.get("productId"), productId);
    }

    public static Specification<Review> hasCustomerId(String customerId) {
        return (root, query, cb) -> cb.equal(root.get("customerId"), customerId);
    }
}