package com.ekko.seller_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SellerMetricsResponse(
        @Schema(description = "Total number of sales", example = "150")
        Long totalSales,

        @Schema(description = "Total revenue", example = "12500000.00")
        BigDecimal totalRevenue,

        @Schema(description = "Average product rating", example = "4.5")
        BigDecimal averageRating,

        @Schema(description = "Total number of reviews", example = "89")
        Long totalReviews,

        @Schema(description = "Number of active products", example = "25")
        Long activeProducts,

        @Schema(description = "Last metrics update timestamp", example = "2024-01-20T14:45:00")
        LocalDateTime updatedAt
) {}
