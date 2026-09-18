package com.contrapposto.app.service;

import com.contrapposto.app.config.StripeProperties;
import com.contrapposto.app.model.BillingPeriod;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.SubscriptionStatus;
import com.contrapposto.app.model.User;
import com.contrapposto.app.repository.UserRepository;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final int TRIAL_PERIOD_DAYS = 30;

    private final StripeProperties stripeProperties;
    private final UserRepository userRepository;

    public SubscriptionServiceImpl(StripeProperties stripeProperties, UserRepository userRepository) {
        this.stripeProperties = stripeProperties;
        this.userRepository = userRepository;
    }

    @Override
    public boolean isConfigured() {
        return stripeProperties.isConfigured();
    }

    @Override
    public boolean isSubscriptionActive(User user) {
        return user.getSubscriptionStatus().isActive();
    }

    @Override
    public String createCheckoutSession(User user, BillingPeriod billingPeriod,
                                        String successUrl, String cancelUrl) {
        if (!isConfigured()) {
            throw new IllegalStateException("Stripe is not configured");
        }

        try {
            String customerId = getOrCreateStripeCustomer(user);
            String priceId = resolvePriceId(user.getRole(), billingPeriod);

            SessionCreateParams params = SessionCreateParams.builder()
                    .setCustomer(customerId)
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(priceId)
                            .setQuantity(1L)
                            .build())
                    .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                            .setTrialPeriodDays((long) TRIAL_PERIOD_DAYS)
                            .build())
                    .setPaymentMethodCollection(SessionCreateParams.PaymentMethodCollection.ALWAYS)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .build();

            Session session = Session.create(params);
            return session.getUrl();

        } catch (StripeException e) {
            throw new RuntimeException("Failed to create Stripe checkout session", e);
        }
    }

    @Override
    public void processWebhookEvent(String payload, String sigHeader) {
        if (stripeProperties.getWebhookSecret().isBlank()) {
            throw new IllegalStateException("Stripe webhook secret is not configured");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeProperties.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            throw new IllegalArgumentException("Invalid Stripe webhook signature", e);
        }

        switch (event.getType()) {
            case "customer.subscription.created", "customer.subscription.updated" ->
                    handleSubscriptionChange((Subscription) deserializeEventObject(event));
            case "customer.subscription.deleted" ->
                    handleSubscriptionDeleted((Subscription) deserializeEventObject(event));
            case "invoice.payment_succeeded" ->
                    handlePaymentSucceeded((Invoice) deserializeEventObject(event));
            case "invoice.payment_failed" ->
                    handlePaymentFailed((Invoice) deserializeEventObject(event));
            default -> { /* ignore unhandled event types */ }
        }
    }

    /**
     * getObject() can come back empty when the event was serialized with a Stripe API
     * version other than the one this SDK release expects. deserializeUnsafe() parses
     * the raw JSON directly against the SDK's model classes and doesn't hit that check.
     */
    private StripeObject deserializeEventObject(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            return deserializer.getObject().get();
        }
        try {
            return deserializer.deserializeUnsafe();
        } catch (EventDataObjectDeserializationException e) {
            throw new IllegalStateException("Could not deserialize Stripe event payload", e);
        }
    }

    private String getOrCreateStripeCustomer(User user) throws StripeException {
        if (user.getStripeCustomerId() != null) {
            return user.getStripeCustomerId();
        }

        Customer customer = Customer.create(
                CustomerCreateParams.builder()
                        .setEmail(user.getEmail())
                        .build()
        );

        user.setStripeCustomerId(customer.getId());
        userRepository.save(user);
        return customer.getId();
    }

    private String resolvePriceId(Role role, BillingPeriod billingPeriod) {
        StripeProperties.Prices prices = stripeProperties.getPrices();
        return switch (role) {
            case MODEL -> billingPeriod == BillingPeriod.MONTHLY
                    ? prices.getModelMonthly()
                    : prices.getModelAnnual();
            case ORGANIZER -> billingPeriod == BillingPeriod.MONTHLY
                    ? prices.getOrganizerMonthly()
                    : prices.getOrganizerAnnual();
            default -> throw new IllegalArgumentException("No pricing defined for role: " + role);
        };
    }

    private void handleSubscriptionChange(Subscription subscription) {
        findUserByCustomerId(subscription.getCustomer()).ifPresent(user -> {
            user.setStripeSubscriptionId(subscription.getId());
            toInstant(subscription.getCurrentPeriodEnd()).ifPresent(user::setCurrentPeriodEndsAt);

            SubscriptionStatus status = switch (subscription.getStatus()) {
                case "trialing" -> {
                    toInstant(subscription.getTrialEnd()).ifPresent(user::setTrialEndsAt);
                    yield SubscriptionStatus.TRIAL;
                }
                case "active" -> SubscriptionStatus.ACTIVE;
                case "past_due", "unpaid", "canceled", "incomplete_expired" -> SubscriptionStatus.LAPSED;
                default -> user.getSubscriptionStatus();
            };

            user.setSubscriptionStatus(status);
            userRepository.save(user);
        });
    }

    private void handleSubscriptionDeleted(Subscription subscription) {
        findUserByCustomerId(subscription.getCustomer()).ifPresent(user -> {
            user.setSubscriptionStatus(SubscriptionStatus.LAPSED);
            userRepository.save(user);
        });
    }

    private void handlePaymentSucceeded(Invoice invoice) {
        if (invoice.getSubscription() == null) return;
        findUserByCustomerId(invoice.getCustomer()).ifPresent(user -> {
            if (user.getSubscriptionStatus() != SubscriptionStatus.TRIAL) {
                user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
                userRepository.save(user);
            }
        });
    }

    private void handlePaymentFailed(Invoice invoice) {
        if (invoice.getSubscription() == null) return;
        findUserByCustomerId(invoice.getCustomer()).ifPresent(user -> {
            user.setSubscriptionStatus(SubscriptionStatus.LAPSED);
            userRepository.save(user);
        });
    }

    private Optional<User> findUserByCustomerId(String customerId) {
        return userRepository.findByStripeCustomerId(customerId);
    }

    private Optional<Instant> toInstant(Long epochSeconds) {
        return Optional.ofNullable(epochSeconds).map(Instant::ofEpochSecond);
    }
}
