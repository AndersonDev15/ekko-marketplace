package com.ekko.payment_service.domain.policy;

import com.ekko.payment_service.application.exception.PaymentAccessDeniedException;
import com.ekko.payment_service.domain.model.Payment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentOwnershipPolicy {

    public void assertCustomerOwns(Payment payment, UUID keycloakId) {
        if (payment.getCustomerId() == null ||
                !payment.getCustomerId().equals(keycloakId)) {
            throw new PaymentAccessDeniedException();
        }
    }

    public void assertGuestOwns(Payment payment, String guestEmail) {
        if (payment.getGuestEmail() == null ||
                !payment.getGuestEmail().equals(guestEmail)) {
            throw new PaymentAccessDeniedException();
        }
    }
}