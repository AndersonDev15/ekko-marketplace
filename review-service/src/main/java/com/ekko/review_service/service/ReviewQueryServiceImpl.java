package com.ekko.review_service.service;

import com.ekko.review_service.entity.EligibleReview;
import com.ekko.review_service.entity.Review;
import com.ekko.review_service.entity.ReviewImage;
import com.ekko.review_service.enums.ReviewStatus;
import com.ekko.review_service.exception.ReviewNotFoundException;
import com.ekko.review_service.mapper.ReviewMapper;
import com.ekko.review_service.repository.EligibleReviewRepository;
import com.ekko.review_service.repository.HelpfulVoteCountProjection;
import com.ekko.review_service.repository.RatingCountProjection;
import com.ekko.review_service.repository.ReviewHelpfulVoteRepository;
import com.ekko.review_service.repository.ReviewImageRepository;
import com.ekko.review_service.repository.ReviewRatingAggregateProjection;
import com.ekko.review_service.repository.ReviewRepository;
import com.ekko.review_service.specification.ReviewSpecifications;
import com.ekko.review_service.web.dto.EligibleToReviewResponse;
import com.ekko.review_service.web.dto.ProductReviewsResponse;
import com.ekko.review_service.web.dto.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewQueryServiceImpl implements ReviewQueryService {

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ReviewHelpfulVoteRepository reviewHelpfulVoteRepository;
    private final EligibleReviewRepository eligibleReviewRepository;
    private final ReviewMapper reviewMapper;

    @Override
    @Transactional(readOnly = true)
    public ProductReviewsResponse getProductReviews(UUID productId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.VISIBLE, pageable);

        ReviewRatingAggregateProjection aggregate =
                reviewRepository.findRatingAggregateByProductId(productId);
        BigDecimal averageRating = aggregate.getAverageRating() == null
                ? BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP)
                : aggregate.getAverageRating().setScale(1, RoundingMode.HALF_UP);
        long totalReviews = aggregate.getReviewCount() == null ? 0L : aggregate.getReviewCount();

        Map<Integer, Long> ratingDistribution = buildRatingDistribution(productId);

        return new ProductReviewsResponse(
                mapWithHelpfulCounts(reviews),
                averageRating,
                totalReviews,
                ratingDistribution);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReviewById(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(ReviewNotFoundException::new);

        // A hidden review must not be distinguishable from a non-existent one in this public endpoint.
        if (review.getStatus() == ReviewStatus.HIDDEN) {
            throw new ReviewNotFoundException();
        }

        return toResponse(review, reviewHelpfulVoteRepository.countByReviewId(reviewId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMyReviews(String customerId, Pageable pageable) {
        Pageable effective = pageable.getSort().isUnsorted()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"))
                : pageable;

        Page<Review> reviews = reviewRepository.findByCustomerId(customerId, effective);
        return mapWithHelpfulCounts(reviews);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EligibleToReviewResponse> getEligibleToReview(String customerId) {
        return eligibleReviewRepository.findEligibleWithoutReview(customerId).stream()
                .map(this::toEligibleToReviewResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> adminGetAllReviews(ReviewStatus status, UUID productId, String customerId, Pageable pageable) {
        List<Specification<Review>> filters = new ArrayList<>();
        if (status != null) {
            filters.add(ReviewSpecifications.hasStatus(status));
        }
        if (productId != null) {
            filters.add(ReviewSpecifications.hasProductId(productId));
        }
        if (customerId != null) {
            filters.add(ReviewSpecifications.hasCustomerId(customerId));
        }
        Specification<Review> spec = filters.stream().reduce(Specification::and).orElse(null);

        Page<Review> reviews = reviewRepository.findAll(spec, pageable);
        return mapWithHelpfulCounts(reviews);
    }

    private Map<Integer, Long> buildRatingDistribution(UUID productId) {
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int rating = 1; rating <= 5; rating++) {
            distribution.put(rating, 0L);
        }
        for (RatingCountProjection entry : reviewRepository.findRatingDistributionByProductId(productId)) {
            distribution.put(entry.getRating(), entry.getCount());
        }
        return distribution;
    }

    private Page<ReviewResponse> mapWithHelpfulCounts(Page<Review> reviews) {
        List<UUID> reviewIds = reviews.getContent().stream().map(Review::getId).toList();
        Map<UUID, Long> countsByReviewId = reviewIds.isEmpty() ? Map.of()
                : reviewHelpfulVoteRepository.countByReviewIdIn(reviewIds).stream()
                        .collect(Collectors.toMap(HelpfulVoteCountProjection::getReviewId, HelpfulVoteCountProjection::getCount));
        return reviews.map(review -> toResponse(review, countsByReviewId.getOrDefault(review.getId(), 0L)));
    }

    private ReviewResponse toResponse(Review review, long helpfulCount) {
        List<ReviewImage> images = reviewImageRepository.findByReviewIdOrderBySortOrderAsc(review.getId());
        return reviewMapper.toResponse(review, images, helpfulCount);
    }

    private EligibleToReviewResponse toEligibleToReviewResponse(EligibleReview eligible) {
        return new EligibleToReviewResponse(eligible.getOrderId(), eligible.getOrderItemId(), eligible.getProductId());
    }
}