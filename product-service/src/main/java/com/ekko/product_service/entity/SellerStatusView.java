package com.ekko.product_service.entity;

import com.ekko.product_service.enums.SellerStatus;
import jakarta.persistence.*;
import jdk.jfr.Name;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Name("seller_status_view")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SellerStatusView {
    @Id
    private UUID sellerKeycloakId;
    @Enumerated(EnumType.STRING)
    private SellerStatus status;
    private String sellerSlug;
    private LocalDateTime updatedAt;
}
