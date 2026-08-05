package com.ekko.seller_service.mapper;

import com.ekko.seller_service.dto.response.*;
import com.ekko.seller_service.entity.*;
import org.springframework.stereotype.Component;

@Component
public class SellerMapper {

    public SellerResponse toResponse(Seller seller) {

        return new SellerResponse(
                seller.getId(),
                seller.getKeycloakId(),
                seller.getStoreName(),
                seller.getEmail(),
                seller.getPhone(),
                seller.getDescription(),
                seller.getLogoUrl(),
                seller.getStatus(),
                seller.getCreatedAt(),
                seller.getUpdatedAt()
        );
    }

    public SellerAddressResponse toAddressResponse(SellerAddress a) {
        return new SellerAddressResponse(
                a.getId(),
                a.getAddressLine(),
                a.getCity(),
                a.getState(),
                a.getCountry(),
                a.getPostalCode(),
                a.getPrimary(),
                a.getCreatedAt()
        );
    }

    public SellerBankAccountResponse toBankAccountResponse(SellerBankAccount b) {
        return new SellerBankAccountResponse(
                b.getId(),
                b.getBankName(),
                b.getAccountType(),
                b.getAccountNumber(),
                b.getAccountHolder(),
                b.getIsPrimary(),
                b.getCreatedAt()
        );
    }

    public SellerDocumentResponse toDocumentResponse(SellerDocument d) {
        return new SellerDocumentResponse(
                d.getId(),
                d.getDocumentType(),
                d.getDocumentUrl(),
                d.getStatus(),
                d.getUploadedAt(),
                d.getReviewedAt(),
                d.getNotes()
        );
    }

    // en SellerMapper
    public SellerMetricsResponse toMetricsResponse(SellerMetrics m) {
        return new SellerMetricsResponse(
                m.getTotalSales(),
                m.getTotalRevenue(),
                m.getAverageRating(),
                m.getTotalReviews(),
                m.getActiveProducts(),
                m.getUpdatedAt()
        );
    }

}