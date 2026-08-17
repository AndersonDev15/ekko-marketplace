package com.ekko.payment_service.infrastructure.persistence.adapter.out;

import com.ekko.payment_service.domain.model.PaymentTransfer;
import com.ekko.payment_service.domain.port.out.PaymentTransferRepositoryPort;
import com.ekko.payment_service.infrastructure.persistence.entity.PaymentTransferEntity;
import com.ekko.payment_service.infrastructure.persistence.mapper.PaymentTransferPersistenceMapper;
import com.ekko.payment_service.infrastructure.persistence.repository.PaymentJpaRepository;
import com.ekko.payment_service.infrastructure.persistence.repository.PaymentTransferJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentTransferRepositoryAdapter implements PaymentTransferRepositoryPort {

    private final PaymentTransferJpaRepository paymentTransferJpaRepository;
    private final PaymentTransferPersistenceMapper paymentTransferPersistenceMapper;
    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    @Transactional
    public PaymentTransfer save(PaymentTransfer transfer) {
        PaymentTransferEntity entity = paymentTransferPersistenceMapper.toEntity(transfer);
        entity.setPayment(paymentJpaRepository.getReferenceById(transfer.getPaymentId()));
        PaymentTransferEntity saved = paymentTransferJpaRepository.save(entity);
        return paymentTransferPersistenceMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentTransfer> findByPaymentId(UUID paymentId) {
        return paymentTransferJpaRepository.findByPaymentId(paymentId).stream()
                .map(paymentTransferPersistenceMapper::toDomain)
                .toList();
    }
}