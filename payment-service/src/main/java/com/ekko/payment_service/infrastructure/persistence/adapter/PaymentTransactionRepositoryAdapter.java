package com.ekko.payment_service.infrastructure.persistence.adapter;

import com.ekko.payment_service.domain.model.PaymentTransaction;
import com.ekko.payment_service.domain.port.out.PaymentTransactionRepositoryPort;
import com.ekko.payment_service.infrastructure.persistence.entity.PaymentEntity;
import com.ekko.payment_service.infrastructure.persistence.entity.PaymentTransactionEntity;
import com.ekko.payment_service.infrastructure.persistence.mapper.PaymentTransactionPersistenceMapper;
import com.ekko.payment_service.infrastructure.persistence.repository.PaymentJpaRepository;
import com.ekko.payment_service.infrastructure.persistence.repository.PaymentTransactionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentTransactionRepositoryAdapter implements PaymentTransactionRepositoryPort {

    private final PaymentTransactionJpaRepository paymentTransactionJpaRepository;
    private final PaymentTransactionPersistenceMapper paymentTransactionPersistenceMapper;
    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    @Transactional
    public PaymentTransaction save(PaymentTransaction transaction) {
        PaymentTransactionEntity entity = paymentTransactionPersistenceMapper.toEntity(transaction);
        entity.setPayment(paymentJpaRepository.getReferenceById(transaction.getPaymentId()));
        PaymentTransactionEntity saved = paymentTransactionJpaRepository.save(entity);
        return paymentTransactionPersistenceMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByStripeEventId(String stripeEventId) {
        return paymentTransactionJpaRepository.existsByStripeEventId(stripeEventId);
    }
}