package com.ekko.seller_service.repository;

import com.ekko.seller_service.entity.SellerDocument;
import com.ekko.seller_service.enums.DocumentStatus;
import com.ekko.seller_service.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SellerDocumentRepository extends JpaRepository<SellerDocument, UUID> {

    List<SellerDocument> findBySellerId(UUID sellerId);

    boolean existsBySellerIdAndDocumentTypeAndStatus(
            UUID sellerId,
            DocumentType documentType,
            DocumentStatus status
    );
}
