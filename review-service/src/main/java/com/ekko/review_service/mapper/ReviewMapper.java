package com.ekko.review_service.mapper;

import com.ekko.review_service.entity.Review;
import com.ekko.review_service.entity.ReviewImage;
import com.ekko.review_service.dto.request.CreateReviewRequest;
import com.ekko.review_service.dto.response.ReviewResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ReviewMapper {

    @Mapping(target = "imageUrls", source = "images")
    default ReviewResponse toResponse(Review review, List<ReviewImage> images) {
        return toResponse(review, images, 0L);
    }

    @Mapping(target = "imageUrls", source = "images")
    ReviewResponse toResponse(Review review, List<ReviewImage> images, long helpfulCount);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", expression = "java(com.ekko.review_service.enums.ReviewStatus.VISIBLE)")
    @Mapping(target = "isVerifiedPurchase", expression = "java(true)")
    @Mapping(target = "reviewedBy", ignore = true)
    @Mapping(target = "reviewedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Review toEntity(CreateReviewRequest request, String customerId);

    default String reviewImageToUrl(ReviewImage image) {
        return image.getUrl();
    }
}