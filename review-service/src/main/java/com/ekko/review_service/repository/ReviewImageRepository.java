package com.ekko.review_service.repository;

import com.ekko.review_service.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, UUID> {

    List<ReviewImage> findByReviewIdOrderBySortOrderAsc(UUID reviewId);

    void deleteByReviewId(UUID reviewId);
}