package com.ekko.product_service.service;

import com.ekko.product_service.entity.SellerStatusView;
import com.ekko.product_service.messaging.dto.consume.SellerSlugChangedEvent;
import com.ekko.product_service.repository.ProductRepository;
import com.ekko.product_service.repository.SellerStatusViewRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerSlugChangedService {

    private final SellerStatusViewRepository sellerStatusViewRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void updateSellerSlug(SellerSlugChangedEvent event) {

        UUID sellerKeycloakId = UUID.fromString(event.keycloakId());

        SellerStatusView view = sellerStatusViewRepository
                .findById(sellerKeycloakId)
                .orElse(new SellerStatusView());

        view.setSellerKeycloakId(sellerKeycloakId);
        view.setSellerSlug(event.slug());
        view.setUpdatedAt(event.changedAt());

        sellerStatusViewRepository.save(view);

        productRepository.updateSellerSlugBySellerKeycloakId(
                sellerKeycloakId,
                event.slug()
        );
    }
}
