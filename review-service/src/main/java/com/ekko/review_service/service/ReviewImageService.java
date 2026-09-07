package com.ekko.review_service.service;

import com.ekko.review_service.dto.response.ReviewImageResponse;
import com.ekko.review_service.entity.Review;
import com.ekko.review_service.entity.ReviewImage;
import com.ekko.review_service.exception.ReviewImageLimitExceededException;
import com.ekko.review_service.exception.ReviewImageNotFoundException;
import com.ekko.review_service.exception.ReviewNotFoundException;
import com.ekko.review_service.exception.ReviewOwnershipException;
import com.ekko.review_service.mapper.ReviewMapper;
import com.ekko.review_service.repository.ReviewImageRepository;
import com.ekko.review_service.repository.ReviewRepository;
import com.ekko.review_service.validator.ReviewEditWindowValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewImageService {

    private static final int MAX_IMAGES_PER_REVIEW = 5;

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final CloudinaryService cloudinaryService;
    private final ReviewMapper reviewMapper;
    private final ReviewEditWindowValidator editWindowValidator;

    @Transactional
    public ReviewImageResponse uploadImage(UUID reviewId, MultipartFile file, String customerId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(ReviewNotFoundException::new);

        assertOwnership(review, customerId);
        editWindowValidator.assertWithinWindow(review.getCreatedAt());

        List<ReviewImage> existing = reviewImageRepository.findByReviewIdOrderBySortOrderAsc(reviewId);
        if (existing.size() >= MAX_IMAGES_PER_REVIEW) {
            throw new ReviewImageLimitExceededException();
        }

        CloudinaryService.UploadResult upload = cloudinaryService.upload(file, "reviews");

        int sortOrder = existing.isEmpty() ? 0 : existing.get(existing.size() - 1).getSortOrder() + 1;

        ReviewImage image = ReviewImage.builder()
                .review(review)
                .url(upload.url())
                .publicId(upload.publicId())
                .sortOrder(sortOrder)
                .createdAt(LocalDateTime.now())
                .build();

        reviewImageRepository.save(image);

        return reviewMapper.toImageResponse(image);
    }

    @Transactional
    public void deleteImage(UUID reviewId, UUID imageId, String customerId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(ReviewNotFoundException::new);

        assertOwnership(review, customerId);
        editWindowValidator.assertWithinWindow(review.getCreatedAt());

        ReviewImage image = reviewImageRepository.findById(imageId)
                .filter(img -> img.getReview().getId().equals(reviewId))
                .orElseThrow(ReviewImageNotFoundException::new);

        reviewImageRepository.delete(image);
        if (image.getPublicId() != null) {
            cloudinaryService.delete(image.getPublicId());
        }
    }

    private void assertOwnership(Review review, String customerId) {
        if (!review.getCustomerId().equals(customerId)) {
            throw new ReviewOwnershipException();
        }
    }
}