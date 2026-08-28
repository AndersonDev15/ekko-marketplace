package com.ekko.seller_service.service;

import com.ekko.seller_service.dto.response.SellerLogoResponse;
import com.ekko.seller_service.dto.response.SellerResponse;
import com.ekko.seller_service.dto.request.SellerUpdateRequest;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerMetrics;
import com.ekko.seller_service.enums.SellerStatus;
import com.ekko.seller_service.exception.SellerNotFoundException;
import com.ekko.seller_service.exception.SellerSuspendedException;
import com.ekko.seller_service.mapper.SellerMapper;
import com.ekko.seller_service.messaging.SellerEventPublisher;
import com.ekko.seller_service.messaging.dto.publish.SellerCreatedEvent;
import com.ekko.seller_service.messaging.dto.publish.SellerSlugChangedEvent;
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
import org.springframework.web.multipart.MultipartFile;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerProfileService {

    private final SellerRepository sellerRepository;
    private final SellerMetricsRepository metricsRepository;
    private final SellerMapper sellerMapper;
    private final SellerEventPublisher sellerEventPublisher;
    private final PlatformTransactionManager transactionManager;
    private final CloudinaryService cloudinaryService;
    private final SellerSlugService sellerSlugService;
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

        boolean slugChanged = false;
        if (request.storeName() != null && !request.storeName().equals(seller.getStoreName())) {
            seller.setStoreName(request.storeName());
            seller.setSlug(sellerSlugService.generateUnique(request.storeName()));
            slugChanged = true;
        }

        seller.setPhone(request.phone());
        seller.setDescription(request.description());

        Seller saved = sellerRepository.save(seller);

        if (slugChanged) {
            sellerEventPublisher.publishSellerSlugChanged(toSlugChangedEvent(saved));
        }

        return sellerMapper.toResponse(saved);
    }

    @Transactional
    public SellerLogoResponse uploadLogo(String keycloakId, MultipartFile file) {
        Seller seller = findByKeycloakIdOrThrow(keycloakId);

        if (seller.getStatus() == SellerStatus.SUSPENDED) {
            throw new SellerSuspendedException(seller.getId());
        }

        if (seller.getLogoPublicId() != null) {
            cloudinaryService.delete(seller.getLogoPublicId());
        }

        CloudinaryService.UploadResult upload = cloudinaryService.upload(file);

        seller.setLogoUrl(upload.url());
        seller.setLogoPublicId(upload.publicId());
        seller.setUpdatedAt(LocalDateTime.now());

        Seller saved = sellerRepository.save(seller);
        return new SellerLogoResponse(saved.getLogoUrl());
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

    private SellerCreatedEvent toSellerCreatedEvent(Seller seller) {
        return new SellerCreatedEvent(
                seller.getId(),
                seller.getKeycloakId(),
                seller.getStoreName(),
                seller.getEmail(),
                seller.getSlug(),
                seller.getStatus(),
                seller.getCreatedAt()
        );
    }

    private SellerSlugChangedEvent toSlugChangedEvent(Seller seller) {
        return new SellerSlugChangedEvent(
                seller.getId(),
                seller.getKeycloakId(),
                seller.getSlug(),
                LocalDateTime.now()
        );
    }

    private SellerResponse createMyProfile(String keycloakId, String email) {
        try {
            Seller created = inNewTransaction(status -> {
                String storeName = "Mi tienda";
                Seller saved = sellerRepository.saveAndFlush(
                        Seller.builder()
                                .keycloakId(keycloakId)
                                .email(email)
                                .storeName("Mi tienda")
                                .slug(sellerSlugService.generateUnique(storeName))
                                .status(SellerStatus.PENDING_REVIEW)
                                .build()
                );

                metricsRepository.saveAndFlush(
                        SellerMetrics.builder()
                                .seller(saved)
                                .build()
                );

                return saved;
            });

            sellerEventPublisher.publishSellerCreated(toSellerCreatedEvent(created));
            return sellerMapper.toResponse(created);

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