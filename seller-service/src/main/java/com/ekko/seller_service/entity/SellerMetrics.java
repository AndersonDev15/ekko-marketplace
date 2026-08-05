package com.ekko.seller_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "seller_metrics")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false, unique = true)
    private Seller seller;

    @Builder.Default
    @Column(name = "total_sales", nullable = false)
    private Long totalSales = 0L;

    @Builder.Default
    @Column(
            name = "total_revenue",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Builder.Default
    @Column(
            name = "average_rating",
            nullable = false,
            precision = 3,
            scale = 2
    )
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_reviews", nullable = false)
    private Long totalReviews = 0L;

    @Builder.Default
    @Column(name = "active_products", nullable = false)
    private Long activeProducts = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}