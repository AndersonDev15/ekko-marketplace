package com.ekko.payment_service.infrastructure.persistence.entity;

import com.ekko.payment_service.domain.model.PaymentStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "guest_email", length = 255)
    private String guestEmail;

    @Column(name = "payment_intent_id", length = 255, unique = true)
    private String paymentIntentId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "payment_method_type", length = 30)
    private String paymentMethodType;

    @Column(name = "payment_method_last4", length = 4)
    private String paymentMethodLast4;

    @Column(name = "stripe_customer_id", length = 255)
    private String stripeCustomerId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<VendorAllocationEntity> allocations = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
