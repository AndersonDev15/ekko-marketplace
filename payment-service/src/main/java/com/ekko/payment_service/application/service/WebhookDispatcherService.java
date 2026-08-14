package com.ekko.payment_service.application.service;

import com.ekko.payment_service.infrastructure.web.webhook.PaymentIntentHandler;
import com.ekko.payment_service.infrastructure.web.webhook.RefundHandler;
import com.ekko.payment_service.infrastructure.web.webhook.VendorAccountHandler;
import com.stripe.model.Account;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.function.BiConsumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookDispatcherService {

    private static final String PAYMENT_INTENT_SUCCEEDED = "payment_intent.succeeded";
    private static final String PAYMENT_INTENT_PAYMENT_FAILED = "payment_intent.payment_failed";
    private static final String PAYMENT_INTENT_CANCELED = "payment_intent.canceled";
    private static final String REFUND_UPDATED = "refund.updated";
    private static final String REFUND_FAILED = "refund.failed";
    private static final String ACCOUNT_UPDATED = "account.updated";

    private final PaymentIntentHandler paymentIntentHandler;
    private final RefundHandler refundHandler;
    private final VendorAccountHandler vendorAccountHandler;

    public void dispatch(Event event) {
        switch (event.getType()) {
            case PAYMENT_INTENT_SUCCEEDED ->
                    dispatchData(event, PaymentIntent.class, paymentIntentHandler::onSucceeded);
            case PAYMENT_INTENT_PAYMENT_FAILED ->
                    dispatchData(event, PaymentIntent.class, paymentIntentHandler::onFailed);
            case PAYMENT_INTENT_CANCELED ->
                    dispatchData(event, PaymentIntent.class, paymentIntentHandler::onCancelled);
            case REFUND_UPDATED ->
                    dispatchData(event, Refund.class, refundHandler::onUpdated);
            case REFUND_FAILED ->
                    dispatchData(event, Refund.class, refundHandler::onFailed);
            case ACCOUNT_UPDATED ->
                    dispatchData(event, Account.class, vendorAccountHandler::onUpdated);
            default -> log.warn("Unhandled Stripe webhook event type: {}", event.getType());
        }
    }

    private <T extends StripeObject> void dispatchData(Event event, Class<T> type, BiConsumer<Event, T> action) {
        Optional<StripeObject> data = event.getDataObjectDeserializer().getObject();
        if (data.isEmpty() || !type.isInstance(data.get())) {
            log.warn("Webhook event {} (id {}) ignored: data object is not a {}",
                    event.getType(), event.getId(), type.getSimpleName());
            return;
        }
        action.accept(event, type.cast(data.get()));
    }
}
