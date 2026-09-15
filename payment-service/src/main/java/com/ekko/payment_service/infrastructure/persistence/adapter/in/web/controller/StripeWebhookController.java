package com.ekko.payment_service.infrastructure.persistence.adapter.in.web.controller;

import com.ekko.payment_service.application.service.WebhookDispatcherService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/stripe")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Stripe Webhooks", description = "Stripe webhook endpoint for receiving payment events")
public class StripeWebhookController {

    private final WebhookDispatcherService webhookDispatcherService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Operation(
            summary = "Handle Stripe webhook",
            description = "Receives and processes Stripe webhook events. Validates the webhook signature before processing."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Webhook processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid Stripe signature")
    })
    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @Parameter(description = "Raw webhook payload", required = true) @RequestBody String payload,
            @Parameter(description = "Stripe signature header", required = true) @RequestHeader("Stripe-Signature") String stripeSignatureHeader) {

        log.info("- STRIPE WEBHOOK RECEIVED -");

        Event event;
        try {
            event = Webhook.constructEvent(
                    payload,
                    stripeSignatureHeader,
                    webhookSecret
            );
        } catch (SignatureVerificationException e) {
            log.warn("Rejected webhook with invalid Stripe signature: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        log.info(
                "Stripe webhook received: type={}, id={}",
                event.getType(),
                event.getId()
        );

        webhookDispatcherService.dispatch(event);

        return ResponseEntity.ok().build();
    }
}