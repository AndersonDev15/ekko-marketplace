package com.ekko.review_service.repository;

import com.ekko.review_service.entity.Review;
import com.ekko.review_service.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID>, JpaSpecificationExecutor<Review> {

    Optional<Review> findByOrderItemIdAndCustomerId(UUID orderItemId, String customerId);

    Page<Review> findByProductIdAndStatus(UUID productId, ReviewStatus status, Pageable pageable);

    Page<Review> findByCustomerId(String customerId, Pageable pageable);

    @Query(value = """
            SELECT AVG(r.rating) AS averageRating, COUNT(*) AS reviewCount
            FROM reviews r
            WHERE r.product_id = :productId AND r.status = 'VISIBLE'
            """, nativeQuery = true)
    ReviewRatingAggregateProjection findRatingAggregateByProductId(@Param("productId") UUID productId);

    @Query(value = """
            SELECT r.rating AS rating, COUNT(*) AS "count"
            FROM reviews r
            WHERE r.product_id = :productId AND r.status = 'VISIBLE'
            GROUP BY r.rating
            """, nativeQuery = true)
    List<RatingCountProjection> findRatingDistributionByProductId(@Param("productId") UUID productId);
}