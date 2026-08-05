package com.ekko.seller_service.service;

import com.ekko.seller_service.support.SellerOperationValidator;
import com.ekko.seller_service.dto.request.SellerDocumentRequest;
import com.ekko.seller_service.dto.response.SellerDocumentResponse;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerDocument;
import com.ekko.seller_service.enums.DocumentStatus;
import com.ekko.seller_service.exception.DocumentAlreadyApprovedException;
import com.ekko.seller_service.exception.DocumentAlreadyPendingException;
import com.ekko.seller_service.exception.SellerNotFoundException;
import com.ekko.seller_service.mapper.SellerMapper;
import com.ekko.seller_service.repository.SellerDocumentRepository;
import com.ekko.seller_service.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final String UNIQUE_VIOLATION = "23505";

    @Transactional
    public SellerDocumentResponse addDocument(
            UUID sellerId,
            SellerDocumentRequest request
    ) {

        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerNotFoundException(sellerId));

        validator.validateCanOperate(seller);

        if (documentRepository.existsBySellerIdAndDocumentTypeAndStatus(
                sellerId,
                request.documentType(),
                DocumentStatus.PENDING
        )) {
            throw new DocumentAlreadyPendingException(
                    request.documentType(),
                    sellerId
            );
        }

        if (documentRepository.existsBySellerIdAndDocumentTypeAndStatus(
                sellerId,
                request.documentType(),
                DocumentStatus.APPROVED
        )) {
            throw new DocumentAlreadyApprovedException(
                    request.documentType(),
                    sellerId
            );
        }

        SellerDocument document = SellerDocument.builder()
                .seller(seller)
                .documentType(request.documentType())
                .documentUrl(request.documentUrl())
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
                        request.documentType(),
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
}