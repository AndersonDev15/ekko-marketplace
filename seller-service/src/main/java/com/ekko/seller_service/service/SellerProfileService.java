package com.ekko.seller_service.service;

import com.ekko.seller_service.dto.response.SellerResponse;
import com.ekko.seller_service.dto.request.SellerUpdateRequest;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerMetrics;
import com.ekko.seller_service.enums.SellerStatus;
import com.ekko.seller_service.exception.SellerNotFoundException;
import com.ekko.seller_service.exception.SellerSuspendedException;
import com.ekko.seller_service.mapper.SellerMapper;
import com.ekko.seller_service.repository.SellerMetricsRepository;
import com.ekko.seller_service.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerProfileService {

    private final SellerRepository sellerRepository;
    private final SellerMetricsRepository metricsRepository;
    private final SellerMapper sellerMapper;
    private static final String UNIQUE_VIOLATION = "23505";

    @Transactional
    public SellerResponse getOrCreateMyProfile(String keycloakId, String email) {
        return sellerRepository.findByKeycloakId(keycloakId)
                .map(sellerMapper::toResponse)
                .orElseGet(() -> createMyProfile(keycloakId, email));
    }

    @Transactional
    public SellerResponse updateMyProfile(String keycloakId, SellerUpdateRequest request) {
        Seller seller = findByKeycloakIdOrThrow(keycloakId);

        if (seller.getStatus() == SellerStatus.SUSPENDED) {
            throw new SellerSuspendedException(seller.getId());
        }

        seller.setStoreName(request.storeName());
        seller.setPhone(request.phone());
        seller.setDescription(request.description());

        return sellerMapper.toResponse(sellerRepository.save(seller));
    }

    @Transactional(readOnly = true)
    public SellerResponse getPublicProfile(UUID sellerId) {
        return sellerRepository.findById(sellerId)
                .map(sellerMapper::toResponse)
                .orElseThrow(() -> new SellerNotFoundException(sellerId));
    }

    // -------------------------------------------------------------------------

    private Seller findByKeycloakIdOrThrow(String keycloakId) {
        return sellerRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new SellerNotFoundException(keycloakId));
    }

    private SellerResponse createMyProfile(String keycloakId, String email) {
        try {
            Seller saved = sellerRepository.saveAndFlush(
                    Seller.builder()
                            .keycloakId(keycloakId)
                            .email(email)
                            .storeName("Mi tienda")
                            .status(SellerStatus.PENDING_REVIEW)
                            .build()
            );

            metricsRepository.saveAndFlush(
                    SellerMetrics.builder()
                            .seller(saved)
                            .build()
            );

            return sellerMapper.toResponse(saved);

        } catch (DataIntegrityViolationException e) {
            Throwable cause = e.getMostSpecificCause();
            if (cause instanceof SQLException sql
                    && UNIQUE_VIOLATION.equals(sql.getSQLState())
                    && sql.getMessage().contains("uk_sellers_keycloak_id")) {
                return sellerRepository.findByKeycloakId(keycloakId)
                        .map(sellerMapper::toResponse)
                        .orElseThrow(() -> new SellerNotFoundException(keycloakId));
            }
            throw e;
        }
    }
}