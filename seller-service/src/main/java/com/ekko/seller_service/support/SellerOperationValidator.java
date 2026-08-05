package com.ekko.seller_service.support;

import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.enums.SellerStatus;
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
}