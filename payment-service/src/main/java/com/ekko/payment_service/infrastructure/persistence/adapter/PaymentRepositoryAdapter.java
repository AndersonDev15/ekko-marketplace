package com.ekko.payment_service.infrastructure.persistence.adapter;

import com.ekko.payment_service.domain.model.Payment;
import com.ekko.payment_service.domain.model.PaymentStatus;
import com.ekko.payment_service.domain.port.out.PaymentRepositoryPort;
import com.ekko.payment_service.infrastructure.persistence.entity.PaymentEntity;
import com.ekko.payment_service.infrastructure.persistence.mapper.PaymentPersistenceMapper;
import com.ekko.payment_service.infrastructure.persistence.repository.PaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepositoryPort {

    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentPersistenceMapper paymentPersistenceMapper;

    @Override
    @Transactional
    public Payment save(Payment payment) {
        PaymentEntity entity = paymentPersistenceMapper.toEntity(payment);
        attachParentReference(entity);
        PaymentEntity saved = paymentJpaRepository.save(entity);
        return paymentPersistenceMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findById(UUID id) {
        return paymentJpaRepository.findById(id)
                .map(paymentPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findByOrderId(UUID orderId) {
        return paymentJpaRepository.findByOrderId(orderId)
                .map(paymentPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findByOrderIdAndStatus(UUID orderId, PaymentStatus status) {
        return paymentJpaRepository.findByOrderIdAndStatus(orderId, status)
                .map(paymentPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findByPaymentIntentId(String paymentIntentId) {
        return paymentJpaRepository.findByPaymentIntentId(paymentIntentId)
                .map(paymentPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findExistingStripeCustomerId(UUID customerId) {
        return paymentJpaRepository
                .findFirstByCustomerIdAndStripeCustomerIdIsNotNullOrderByCreatedAtDesc(customerId)
                .map(PaymentEntity::getStripeCustomerId);
    }

    private void attachParentReference(PaymentEntity entity) {
        if (entity.getAllocations() != null) {
            entity.getAllocations().forEach(allocation -> allocation.setPayment(entity));
        }
    }
}
