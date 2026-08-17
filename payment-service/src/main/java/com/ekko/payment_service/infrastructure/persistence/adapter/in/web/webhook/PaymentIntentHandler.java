package com.ekko.payment_service.infrastructure.persistence.adapter.in.web.webhook;

import com.ekko.payment_service.domain.command.HandlePaymentCancelledCommand;
import com.ekko.payment_service.domain.command.HandlePaymentFailedCommand;
import com.ekko.payment_service.domain.command.HandlePaymentSucceededCommand;
import com.ekko.payment_service.domain.enums.PaymentFailureReason;
import com.ekko.payment_service.domain.port.in.HandlePaymentCancelledUseCase;
import com.ekko.payment_service.domain.port.in.HandlePaymentFailedUseCase;
import com.ekko.payment_service.domain.port.in.HandlePaymentSucceededUseCase;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.StripeError;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class PaymentIntentHandler {

    private final HandlePaymentSucceededUseCase handlePaymentSucceededUseCase;
    private final HandlePaymentFailedUseCase handlePaymentFailedUseCase;
    private final HandlePaymentCancelledUseCase handlePaymentCancelledUseCase;

    public void onSucceeded(Event stripeEvent, PaymentIntent paymentIntent) {
        String paymentMethodType = resolvePaymentMethodType(paymentIntent);
        String paymentMethodLast4 = resolvePaymentMethodLast4(paymentIntent);
        LocalDateTime paidAt = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(stripeEvent.getCreated()),
                ZoneOffset.UTC);

        handlePaymentSucceededUseCase.execute(new HandlePaymentSucceededCommand(
                paymentIntent.getId(),
                stripeEvent.getId(),
                paymentMethodType,
                paymentMethodLast4,
                paidAt));
    }

    public void onFailed(Event stripeEvent, PaymentIntent paymentIntent) {
        StripeError lastError = paymentIntent.getLastPaymentError();

        // TODO: mapear a DECLINED/FRAUD/TIMEOUT a partir de decline_code/failure_code reales de Stripe
        // (StripeError.getDeclineCode() / getCode()) cuando se disponga de un mapeo verificado.
        // Por ahora ERROR como default seguro.
        PaymentFailureReason reason = PaymentFailureReason.ERROR;
        String errorMessage = lastError != null ? lastError.getMessage() : null;

        handlePaymentFailedUseCase.execute(new HandlePaymentFailedCommand(
                paymentIntent.getId(),
                stripeEvent.getId(),
                reason,
                errorMessage));
    }

    public void onCancelled(Event stripeEvent, PaymentIntent paymentIntent) {
        handlePaymentCancelledUseCase.execute(new HandlePaymentCancelledCommand(
                paymentIntent.getId(),
                stripeEvent.getId()));
    }

    private String resolvePaymentMethodType(PaymentIntent paymentIntent) {
        PaymentMethod paymentMethod = paymentIntent.getPaymentMethodObject();
        if (paymentMethod != null) {
            return paymentMethod.getType();
        }
        Charge charge = paymentIntent.getLatestChargeObject();
        if (charge != null && charge.getPaymentMethodDetails() != null) {
            return charge.getPaymentMethodDetails().getType();
        }
        return null;
    }

    private String resolvePaymentMethodLast4(PaymentIntent paymentIntent) {
        PaymentMethod paymentMethod = paymentIntent.getPaymentMethodObject();
        if (paymentMethod != null && paymentMethod.getCard() != null) {
            return paymentMethod.getCard().getLast4();
        }
        Charge charge = paymentIntent.getLatestChargeObject();
        if (charge != null
                && charge.getPaymentMethodDetails() != null
                && charge.getPaymentMethodDetails().getCard() != null) {
            return charge.getPaymentMethodDetails().getCard().getLast4();
        }
        return null;
    }
}
