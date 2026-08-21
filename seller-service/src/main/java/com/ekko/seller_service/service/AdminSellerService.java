package com.ekko.seller_service.service;

import com.ekko.seller_service.dto.request.ReviewDocumentRequest;
import com.ekko.seller_service.dto.request.UpdateSellerStatusRequest;
import com.ekko.seller_service.dto.response.SellerDetailResponse;
import com.ekko.seller_service.dto.response.SellerDocumentResponse;
import com.ekko.seller_service.dto.response.SellerResponse;
import com.ekko.seller_service.dto.response.SellerSummaryResponse;
import com.ekko.seller_service.entity.*;
import com.ekko.seller_service.enums.DocumentStatus;
import com.ekko.seller_service.enums.SellerStatus;
import com.ekko.seller_service.exception.*;
import com.ekko.seller_service.mapper.SellerMapper;
import com.ekko.seller_service.messaging.SellerEventPublisher;
import com.ekko.seller_service.messaging.dto.publish.SellerDocumentReviewEvent;
import com.ekko.seller_service.messaging.dto.publish.SellerStatusChangedEvent;
import com.ekko.seller_service.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminSellerService {

    private final SellerRepository sellerRepository;
    private final SellerDocumentRepository sellerDocumentRepository;
    private final SellerMetricsRepository sellerMetricsRepository;
    private final SellerMapper sellerMapper;
    private final SellerEventPublisher sellerEventPublisher;

    public Page<SellerSummaryResponse> getAllSellers(SellerStatus status,
                                                     LocalDateTime from,
                                                     LocalDateTime to,
                                                     Pageable pageable) {
        return sellerRepository.findAllFiltered(status, from, to, pageable)
                .map(this::toSummaryResponse);
    }

    public SellerDetailResponse getSellerDetail(UUID sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerNotFoundException(sellerId));

        List<SellerDocumentResponse> documents = sellerDocumentRepository.findBySellerId(sellerId)
                .stream()
                .map(sellerMapper::toDocumentResponse)
                .toList();

        SellerMetrics metrics = sellerMetricsRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new SellerMetricsNotFoundException(sellerId));

        return toDetailResponse(seller, documents, metrics);
    }

    @Transactional
    public SellerResponse updateSellerStatus(UUID sellerId, UpdateSellerStatusRequest request) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerNotFoundException(sellerId));

        SellerStatus current = seller.getStatus();
        SellerStatus target = request.status();

        if (!current.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(current, target);
        }

        seller.setStatus(target);
        Seller saved = sellerRepository.save(seller);
        sellerEventPublisher.publishSellerStatusChanged(toSellerStatusChangedEvent(saved, current, target));
        return sellerMapper.toResponse(saved);
    }

    @Transactional
    public SellerDocumentResponse reviewDocument(UUID documentId, ReviewDocumentRequest request, Jwt jwt) {
        SellerDocument document = sellerDocumentRepository.findById(documentId)
                .orElseThrow(() -> new SellerDocumentNotFoundException(documentId));

        if (document.getStatus() != DocumentStatus.PENDING) {
            throw new DocumentAlreadyReviewedException(documentId);
        }

        document.setStatus(request.status());
        document.setNotes(request.notes());
        document.setReviewedAt(LocalDateTime.now());
        document.setReviewedBy(jwt.getSubject());
        SellerDocument reviewed = sellerDocumentRepository.save(document);
        sellerEventPublisher.publishSellerDocumentReview(toSellerDocumentReviewEvent(reviewed));
        return sellerMapper.toDocumentResponse(reviewed);
    }

    private SellerSummaryResponse toSummaryResponse(Seller s) {
        return new SellerSummaryResponse(s.getId(), s.getStoreName(), s.getEmail(),
                s.getStatus(), s.getCreatedAt());
    }

    private SellerDetailResponse toDetailResponse(Seller s, List<SellerDocumentResponse> documents, SellerMetrics metrics) {
        return new SellerDetailResponse(s.getId(), s.getKeycloakId(), s.getStoreName(),
                s.getEmail(), s.getPhone(), s.getDescription(), s.getLogoUrl(),
                s.getStatus(), s.getCreatedAt(), s.getUpdatedAt(), documents, sellerMapper.toMetricsResponse(metrics));
    }

    private SellerStatusChangedEvent toSellerStatusChangedEvent(
            Seller seller,
            SellerStatus previousStatus,
            SellerStatus newStatus) {
        return new SellerStatusChangedEvent(
                seller.getId(),
                seller.getKeycloakId(),
                seller.getEmail(),
                previousStatus,
                newStatus,
                LocalDateTime.now()
        );
    }

    private SellerDocumentReviewEvent toSellerDocumentReviewEvent(SellerDocument document) {
        return new SellerDocumentReviewEvent(
                document.getSeller().getId(),
                document.getSeller().getEmail(),
                document.getSeller().getKeycloakId(),
                document.getId(),
                document.getDocumentType(),
                document.getStatus(),
                document.getReviewedAt(),
                document.getNotes()
        );
    }
}





