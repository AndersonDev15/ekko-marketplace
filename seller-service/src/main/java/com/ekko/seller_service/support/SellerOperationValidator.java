package com.ekko.seller_service.support;

import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.enums.SellerStatus;
import com.ekko.seller_service.exception.SellerAlreadyActiveException;
import com.ekko.seller_service.exception.SellerPendingException;
import com.ekko.seller_service.exception.SellerSuspendedException;
import org.springframework.stereotype.Component;

@Component
public class SellerOperationValidator {

    public void validateCanOperate(Seller seller) {
        if (seller.getStatus() == SellerStatus.SUSPENDED) {
            throw new SellerSuspendedException(seller.getId());
        }
        if (seller.getStatus() == SellerStatus.PENDING_REVIEW) {
            throw new SellerPendingException(seller.getId());
        }
    }

    public void validateCanEditProfile(Seller seller) {
        if (seller.getStatus() == SellerStatus.SUSPENDED) {
            throw new SellerSuspendedException(seller.getId());
        }
        if (seller.getStatus() == SellerStatus.ACTIVE) {
            throw new SellerAlreadyActiveException(seller.getId());
        }
        // PENDING_REVIEW puede editar libremente — es el flujo normal de onboarding
    }
}