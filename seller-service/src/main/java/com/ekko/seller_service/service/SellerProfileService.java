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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerProfileService {

    private final SellerRepository sellerRepository;
    private final SellerMetricsRepository metricsRepository;
    private final SellerMapper sellerMapper;
    private final PlatformTransactionManager transactionManager;
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
            return inNewTransaction(status -> {
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
            });

        } catch (DataIntegrityViolationException e) {
            Throwable cause = e.getMostSpecificCause();
            if (cause instanceof SQLException sql
                    && UNIQUE_VIOLATION.equals(sql.getSQLState())
                    && sql.getMessage().contains("uk_sellers_keycloak_id")) {
                return inNewTransaction(status -> sellerRepository.findByKeycloakId(keycloakId)
                        .map(sellerMapper::toResponse)
                        .orElseThrow(() -> new SellerNotFoundException(keycloakId)));
            }
            throw e;
        }
    }

    /**
     * Ejecuta el callback en una transacción física separada (REQUIRES_NEW).
     * Necesario en la creación concurrente: si el INSERT falla por unicidad,
     * Postgres aborta la transacción actual y no permite más comandos; el fallback
     * de re-lectura debe correr en una transacción fresca.
     */
    private <T> T inNewTransaction(TransactionCallback<T> callback) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template.execute(callback);
    }
}