package com.ekko.seller_service.support;

import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerDocument;
import com.ekko.seller_service.enums.DocumentStatus;
import com.ekko.seller_service.enums.DocumentType;

import java.time.LocalDateTime;
import java.util.UUID;

public final class DocumentTestDataBuilder {

    private UUID id = UUID.randomUUID();
    private Seller seller;
    private DocumentType documentType = DocumentType.ID_CARD;
    private String objectKey = "documents/" + UUID.randomUUID() + "/doc.pdf";
    private DocumentStatus status = DocumentStatus.PENDING;
    private LocalDateTime uploadedAt = LocalDateTime.now();
    private LocalDateTime reviewedAt;
    private String reviewedBy;
    private String notes;

    private DocumentTestDataBuilder() {
    }

    public static DocumentTestDataBuilder aDocument() {
        return new DocumentTestDataBuilder();
    }

    public DocumentTestDataBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public DocumentTestDataBuilder withSeller(Seller seller) {
        this.seller = seller;
        return this;
    }

    public DocumentTestDataBuilder withDocumentType(DocumentType documentType) {
        this.documentType = documentType;
        return this;
    }

    public DocumentTestDataBuilder withObjectKey(String objectKey) {
        this.objectKey = objectKey;
        return this;
    }

    public DocumentTestDataBuilder withStatus(DocumentStatus status) {
        this.status = status;
        return this;
    }

    public DocumentTestDataBuilder pending() {
        return withStatus(DocumentStatus.PENDING);
    }

    public DocumentTestDataBuilder approved() {
        return withStatus(DocumentStatus.APPROVED);
    }

    public DocumentTestDataBuilder rejected() {
        return withStatus(DocumentStatus.REJECTED);
    }

    public DocumentTestDataBuilder withUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
        return this;
    }

    public DocumentTestDataBuilder withReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
        return this;
    }

    public DocumentTestDataBuilder withReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
        return this;
    }

    public DocumentTestDataBuilder withNotes(String notes) {
        this.notes = notes;
        return this;
    }

    public SellerDocument build() {
        return SellerDocument.builder()
                .id(id)
                .seller(seller)
                .documentType(documentType)
                .objectKey(objectKey)
                .status(status)
                .uploadedAt(uploadedAt)
                .reviewedAt(reviewedAt)
                .reviewedBy(reviewedBy)
                .notes(notes)
                .build();
    }
}
