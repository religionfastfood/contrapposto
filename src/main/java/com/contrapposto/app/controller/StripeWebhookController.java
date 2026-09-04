package com.contrapposto.app.controller;

import com.contrapposto.app.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final SubscriptionService subscriptionService;

    public StripeWebhookController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        try {
            subscriptionService.processWebhookEvent(payload, sigHeader);
            return ResponseEntity.ok("OK");
        } catch (IllegalArgumentException e) {
            // Signature verification failed
            log.warn("Stripe webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(400).body("Invalid signature");
        } catch (IllegalStateException e) {
            // Webhook secret not configured
            log.error("Stripe webhook secret is not configured", e);
            return ResponseEntity.status(500).body("Webhook not configured");
        } catch (Exception e) {
            log.error("Stripe webhook processing failed", e);
            return ResponseEntity.status(500).body("Webhook processing failed");
        }
    }
}
