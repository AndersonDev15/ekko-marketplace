package com.ekko.payment_service.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_vendor_allocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorAllocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private PaymentEntity payment;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "gross_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "application_fee_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal applicationFeeAmount;

    @Column(name = "net_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
