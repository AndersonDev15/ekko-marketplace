package com.ekko.product_service.util;

import com.ekko.product_service.entity.SellerStatusView;
import com.ekko.product_service.enums.SellerStatus;
import com.ekko.product_service.exception.SellerNotOperationalException;
import com.ekko.product_service.repository.SellerStatusViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SellerStatusValidator {

    private final SellerStatusViewRepository repository;

    public SellerStatusView validateCanOperate(UUID sellerKeycloakId) {
        SellerStatusView view = repository.findById(sellerKeycloakId)
                .orElseThrow(() -> new SellerNotOperationalException(sellerKeycloakId));

        if (view.getStatus() != SellerStatus.ACTIVE) {
            throw new SellerNotOperationalException(sellerKeycloakId);
        }

        return view;
    }
}