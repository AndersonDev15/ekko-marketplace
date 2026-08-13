package com.ekko.payment_service.application.service;

import com.ekko.payment_service.domain.model.Refund;
import com.ekko.payment_service.domain.port.out.RefundRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefundTransactionService {

    private final RefundRepositoryPort refundRepositoryPort;

    @Transactional
    public Refund commitRefund(Refund refund) {
        return refundRepositoryPort.save(refund);
    }
}