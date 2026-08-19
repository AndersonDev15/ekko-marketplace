package com.ekko.seller_service.service;

import com.ekko.seller_service.dto.response.DocumentDownloadResponse;
import com.ekko.seller_service.dto.response.SellerDocumentResponse;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerDocument;
import com.ekko.seller_service.enums.DocumentStatus;
import com.ekko.seller_service.enums.DocumentType;
import com.ekko.seller_service.exception.DocumentAlreadyApprovedException;
import com.ekko.seller_service.exception.DocumentAlreadyPendingException;
import com.ekko.seller_service.exception.SellerDocumentNotFoundException;
import com.ekko.seller_service.exception.SellerNotFoundException;
import com.ekko.seller_service.mapper.SellerMapper;
import com.ekko.seller_service.repository.SellerDocumentRepository;
import com.ekko.seller_service.repository.SellerRepository;
import com.ekko.seller_service.support.SellerOperationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerDocumentService {

    private final SellerRepository sellerRepository;
    private final SellerDocumentRepository documentRepository;
    private final SellerOperationValidator validator;
    private final SellerMapper sellerMapper;
    private final MinioService minioService;

    private static final String UNIQUE_VIOLATION = "23505";

    @Transactional
    public SellerDocumentResponse uploadDocument(
            UUID sellerId,
            DocumentType documentType,
            MultipartFile file
    ) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerNotFoundException(sellerId));

        validator.validateCanOperate(seller);

        assertNoExistingDocument(sellerId, documentType);

        String objectKey = minioService.upload(file);

        try {
            return addDocument(sellerId, documentType, objectKey);
        } catch (RuntimeException e) {
            try {
                minioService.delete(objectKey);
            } catch (RuntimeException ignored) {
            }
            throw e;
        }
    }

    @Transactional
    public SellerDocumentResponse addDocument(
            UUID sellerId,
            DocumentType documentType,
            String objectKey
    ) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerNotFoundException(sellerId));

        validator.validateCanOperate(seller);

        assertNoExistingDocument(sellerId, documentType);

        SellerDocument document = SellerDocument.builder()
                .seller(seller)
                .documentType(documentType)
                .objectKey(objectKey)
                .status(DocumentStatus.PENDING)
                .build();

        try {

            SellerDocument saved =
                    documentRepository.saveAndFlush(document);

            return sellerMapper.toDocumentResponse(saved);

        } catch (DataIntegrityViolationException e) {

            Throwable cause = e.getMostSpecificCause();

            if (cause instanceof SQLException sql
                    && UNIQUE_VIOLATION.equals(sql.getSQLState())
                    && (sql.getMessage().contains("uk_seller_document_pending")
                    || sql.getMessage().contains("uk_seller_document_type"))) {

                throw new DocumentAlreadyPendingException(
                        documentType,
                        sellerId
                );
            }

            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<SellerDocumentResponse> getMyDocuments(UUID sellerId) {

        return documentRepository.findBySellerId(sellerId)
                .stream()
                .map(sellerMapper::toDocumentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentDownloadResponse getDownloadUrl(UUID sellerId, UUID documentId) {

        SellerDocument document = documentRepository.findByIdAndSellerId(documentId, sellerId)
                .orElseThrow(() -> new SellerDocumentNotFoundException(documentId, sellerId));

        MinioService.PresignedUrl presigned = minioService.generatePresignedUrl(document.getObjectKey());

        return new DocumentDownloadResponse(document.getId(), presigned.url(), presigned.expiresAt());
    }

    private void assertNoExistingDocument(UUID sellerId, DocumentType documentType) {
        if (documentRepository.existsBySellerIdAndDocumentTypeAndStatus(
                sellerId,
                documentType,
                DocumentStatus.PENDING
        )) {
            throw new DocumentAlreadyPendingException(documentType, sellerId);
        }

        if (documentRepository.existsBySellerIdAndDocumentTypeAndStatus(
                sellerId,
                documentType,
                DocumentStatus.APPROVED
        )) {
            throw new DocumentAlreadyApprovedException(documentType, sellerId);
        }
    }
}