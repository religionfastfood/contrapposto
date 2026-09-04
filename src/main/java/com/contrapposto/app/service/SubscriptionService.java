package com.contrapposto.app.service;

import com.contrapposto.app.model.BillingPeriod;
import com.contrapposto.app.model.User;

public interface SubscriptionService {

    /**
     * Creates a Stripe Checkout session for the given user and billing period.
     * Returns the Stripe-hosted checkout URL to redirect the user to.
     */
    String createCheckoutSession(User user, BillingPeriod billingPeriod, String successUrl, String cancelUrl);

    /**
     * Processes an incoming Stripe webhook event. Verifies the signature and
     * updates subscription state in the database accordingly.
     */
    void processWebhookEvent(String payload, String sigHeader);

    /**
     * Returns true if the user's subscription is in an active state (TRIAL or ACTIVE).
     */
    boolean isSubscriptionActive(User user);

    /**
     * Returns true if the Stripe integration is configured (secret key is set).
     */
    boolean isConfigured();
}
