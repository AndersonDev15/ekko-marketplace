package com.ekko.product_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "seller_snapshot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerSnapshot {

    @Id
    @Column(name = "seller_keycloak_id")
    private UUID sellerKeycloakId;

    @Column(name = "store_name", nullable = false, length = 255)
    private String storeName;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
