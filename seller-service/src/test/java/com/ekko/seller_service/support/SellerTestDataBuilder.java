package com.ekko.seller_service.support;

import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.enums.SellerStatus;

import java.util.UUID;

public final class SellerTestDataBuilder {

    private UUID id;
    private String keycloakId = UUID.randomUUID().toString();
    private String storeName = "Mi tienda";
    private String email = "seller-" + UUID.randomUUID() + "@ekko.test";
    private String phone = "3001234567";
    private String description;
    private String logoUrl;
    private SellerStatus status = SellerStatus.ACTIVE;

    private SellerTestDataBuilder() {
    }

    public static SellerTestDataBuilder aSeller() {
        return new SellerTestDataBuilder();
    }

    public SellerTestDataBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public SellerTestDataBuilder withKeycloakId(String keycloakId) {
        this.keycloakId = keycloakId;
        return this;
    }

    public SellerTestDataBuilder withStoreName(String storeName) {
        this.storeName = storeName;
        return this;
    }

    public SellerTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public SellerTestDataBuilder withPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public SellerTestDataBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public SellerTestDataBuilder withLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
        return this;
    }

    public SellerTestDataBuilder withLogoPublicId(String logoPublicId) {
        // We'll set this directly on the built object since Seller builder doesn't have it
        return this;
    }

    public SellerTestDataBuilder withStatus(SellerStatus status) {
        this.status = status;
        return this;
    }

    public SellerTestDataBuilder active() {
        return withStatus(SellerStatus.ACTIVE);
    }

    public SellerTestDataBuilder suspended() {
        return withStatus(SellerStatus.SUSPENDED);
    }

    public SellerTestDataBuilder pendingReview() {
        return withStatus(SellerStatus.PENDING_REVIEW);
    }

    public Seller build() {
        return Seller.builder()
                .id(id)
                .keycloakId(keycloakId)
                .storeName(storeName)
                .email(email)
                .phone(phone)
                .description(description)
                .logoUrl(logoUrl)
                .status(status)
                .build();
    }
}
