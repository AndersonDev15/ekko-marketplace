package com.ekko.payment_service.application.service;

import com.ekko.payment_service.application.exception.RefundAlreadyProcessedException;
import com.ekko.payment_service.application.exception.RefundNotFoundException;
import com.ekko.payment_service.domain.enums.RefundStatus;
import com.ekko.payment_service.domain.model.Refund;
import com.ekko.payment_service.domain.port.in.RejectRefundUseCase;
import com.ekko.payment_service.domain.port.out.RefundRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RejectRefundService implements RejectRefundUseCase {

    private final RefundRepositoryPort refundRepositoryPort;

    @Override
    public Refund execute(UUID refundId) {
        Refund refund = refundRepositoryPort.findById(refundId)
                .orElseThrow(() -> new RefundNotFoundException(refundId));

        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new RefundAlreadyProcessedException(refund.getId(), refund.getStatus());
        }

        refund.markRejected();

        return refundRepositoryPort.save(refund);
    }
}