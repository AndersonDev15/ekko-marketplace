package com.ekko.payment_service.infrastructure.persistence.adapter.out;

import com.ekko.payment_service.domain.model.Refund;
import com.ekko.payment_service.domain.enums.RefundStatus;
import com.ekko.payment_service.domain.port.out.RefundRepositoryPort;
import com.ekko.payment_service.infrastructure.persistence.entity.RefundEntity;
import com.ekko.payment_service.infrastructure.persistence.mapper.RefundPersistenceMapper;
import com.ekko.payment_service.infrastructure.persistence.repository.PaymentJpaRepository;
import com.ekko.payment_service.infrastructure.persistence.repository.RefundJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundRepositoryAdapter implements RefundRepositoryPort {

    private final RefundJpaRepository refundJpaRepository;
    private final RefundPersistenceMapper refundPersistenceMapper;
    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    @Transactional
    public Refund save(Refund refund) {
        RefundEntity entity = refundPersistenceMapper.toEntity(refund);
        entity.setPayment(paymentJpaRepository.getReferenceById(refund.getPaymentId()));
        RefundEntity saved = refundJpaRepository.save(entity);
        return refundPersistenceMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumSucceededAmountByPaymentId(UUID paymentId) {
        BigDecimal sum = refundJpaRepository.sumAmountByPaymentIdAndStatus(paymentId, RefundStatus.SUCCEEDED);
        return Optional.ofNullable(sum).orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Refund> findByStripeRefundId(String stripeRefundId) {
        return refundJpaRepository.findByStripeRefundId(stripeRefundId)
                .map(refundPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Refund> findById(UUID id) {
        return refundJpaRepository.findById(id)
                .map(refundPersistenceMapper::toDomain);
    }
}