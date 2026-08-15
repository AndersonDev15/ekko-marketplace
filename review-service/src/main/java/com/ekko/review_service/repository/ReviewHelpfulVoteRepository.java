package com.ekko.review_service.repository;

import com.ekko.review_service.entity.ReviewHelpfulVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewHelpfulVoteRepository extends JpaRepository<ReviewHelpfulVote, UUID> {

    Optional<ReviewHelpfulVote> findByReviewIdAndCustomerId(UUID reviewId, String customerId);

    long countByReviewId(UUID reviewId);

    @Query(value = """
            SELECT v.review_id AS "reviewId", COUNT(*) AS "count"
            FROM review_helpful_votes v
            WHERE v.review_id IN (:reviewIds)
            GROUP BY v.review_id
            """, nativeQuery = true)
    List<HelpfulVoteCountProjection> countByReviewIdIn(@Param("reviewIds") List<UUID> reviewIds);
}