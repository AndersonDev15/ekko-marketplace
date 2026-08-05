package com.ekko.seller_service.enums;

import java.util.Map;
import java.util.Set;

public enum SellerStatus {
    PENDING_REVIEW, ACTIVE, SUSPENDED;

    private static final Map<SellerStatus, Set<SellerStatus>> ALLOWED_TRANSITIONS = Map.of(
            PENDING_REVIEW, Set.of(ACTIVE),
            ACTIVE, Set.of(SUSPENDED),
            SUSPENDED, Set.of(ACTIVE)
    );

    public boolean canTransitionTo(SellerStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
