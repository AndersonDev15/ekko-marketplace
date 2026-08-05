package com.ekko.seller_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SellerMetricsResponse(
        Long totalSales,
        BigDecimal totalRevenue,
        BigDecimal averageRating,
        Long totalReviews,
        Long activeProducts,
        LocalDateTime updatedAt
) {}
