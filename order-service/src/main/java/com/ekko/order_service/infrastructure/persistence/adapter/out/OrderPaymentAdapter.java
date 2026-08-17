package com.ekko.order_service.infrastructure.persistence.adapter.out;

import com.ekko.order_service.application.exception.OrderNotFoundException;
import com.ekko.order_service.domain.model.PaymentData;
import com.ekko.order_service.domain.port.out.OrderPaymentPort;
import com.ekko.order_service.infrastructure.persistence.entity.OrderEntity;
import com.ekko.order_service.infrastructure.persistence.entity.OrderPaymentEntity;
import com.ekko.order_service.infrastructure.persistence.enums.PaymentStatus;
import com.ekko.order_service.infrastructure.persistence.repository.OrderJpaRepository;
import com.ekko.order_service.infrastructure.persistence.repository.OrderPaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderPaymentAdapter implements OrderPaymentPort {

    private final OrderPaymentJpaRepository orderPaymentJpaRepository;
    private final OrderJpaRepository orderJpaRepository;

    @Override
    @Transactional
    public void recordCompletedPayment(UUID orderId, PaymentData paymentData) {
        OrderEntity order = orderJpaRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId.toString()));

        LocalDateTime now = LocalDateTime.now();

        OrderPaymentEntity payment = OrderPaymentEntity.builder()
                .order(order)
                .paymentId(paymentData.paymentId() != null ? paymentData.paymentId().toString() : null)
                .amount(paymentData.amount())
                .currency(paymentData.currency())
                .status(PaymentStatus.COMPLETED)
                .isCurrent(true)
                .paidAt(paymentData.paidAt())
                .createdAt(now)
                .updatedAt(now)
                .build();

        orderPaymentJpaRepository.save(payment);
    }
}