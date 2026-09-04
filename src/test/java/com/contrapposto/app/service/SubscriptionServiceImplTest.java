package com.contrapposto.app.service;

import com.contrapposto.app.config.StripeProperties;
import com.contrapposto.app.model.BillingPeriod;
import com.contrapposto.app.model.SubscriptionStatus;
import com.contrapposto.app.model.User;
import com.contrapposto.app.repository.UserRepository;
import net.jqwik.api.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriptionServiceImplTest {

    // Fields initialized directly so both JUnit 5 (@Test) and jqwik (@Property)
    // get a fresh instance — @BeforeEach is not invoked by jqwik's test engine.
    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final StripeProperties stripeProperties = new StripeProperties();
    private final SubscriptionServiceImpl subscriptionService =
            new SubscriptionServiceImpl(stripeProperties, userRepository);

    // --- isConfigured ---

    @Test
    void isConfigured_withNoKey_returnsFalse() {
        assertThat(subscriptionService.isConfigured()).isFalse();
    }

    @Test
    void isConfigured_withKey_returnsTrue() {
        StripeProperties props = new StripeProperties();
        props.setSecretKey("sk_test_abc123");
        SubscriptionServiceImpl service = new SubscriptionServiceImpl(props, userRepository);
        assertThat(service.isConfigured()).isTrue();
    }

    // --- isSubscriptionActive ---

    @Test
    void isSubscriptionActive_trial_returnsTrue() {
        assertThat(subscriptionService.isSubscriptionActive(userWithStatus(SubscriptionStatus.TRIAL))).isTrue();
    }

    @Test
    void isSubscriptionActive_active_returnsTrue() {
        assertThat(subscriptionService.isSubscriptionActive(userWithStatus(SubscriptionStatus.ACTIVE))).isTrue();
    }

    @Test
    void isSubscriptionActive_lapsed_returnsFalse() {
        assertThat(subscriptionService.isSubscriptionActive(userWithStatus(SubscriptionStatus.LAPSED))).isFalse();
    }

    @Test
    void isSubscriptionActive_none_returnsFalse() {
        assertThat(subscriptionService.isSubscriptionActive(userWithStatus(SubscriptionStatus.NONE))).isFalse();
    }

    // --- PBT: isSubscriptionActive invariants ---

    @Property
    void activeStatuses_alwaysReturnTrue(@ForAll("activeStatuses") SubscriptionStatus status) {
        assertThat(subscriptionService.isSubscriptionActive(userWithStatus(status))).isTrue();
    }

    @Property
    void inactiveStatuses_alwaysReturnFalse(@ForAll("inactiveStatuses") SubscriptionStatus status) {
        assertThat(subscriptionService.isSubscriptionActive(userWithStatus(status))).isFalse();
    }

    @Provide
    Arbitrary<SubscriptionStatus> activeStatuses() {
        return Arbitraries.of(SubscriptionStatus.TRIAL, SubscriptionStatus.ACTIVE);
    }

    @Provide
    Arbitrary<SubscriptionStatus> inactiveStatuses() {
        return Arbitraries.of(SubscriptionStatus.LAPSED, SubscriptionStatus.NONE);
    }

    // --- createCheckoutSession: requires Stripe config ---

    @Test
    void createCheckoutSession_withoutStripeConfig_throwsIllegalState() {
        assertThatThrownBy(() ->
                subscriptionService.createCheckoutSession(
                        userWithStatus(SubscriptionStatus.NONE),
                        BillingPeriod.MONTHLY,
                        "http://localhost/success",
                        "http://localhost/cancel"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not configured");
    }

    // --- processWebhookEvent: requires webhook secret ---

    @Test
    void processWebhookEvent_withoutWebhookSecret_throwsIllegalState() {
        assertThatThrownBy(() ->
                subscriptionService.processWebhookEvent("{}", "sig"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not configured");
    }

    // --- processWebhookEvent: event handling ---
    // The SDK's integration API version ("2024-06-20") is hardcoded inside
    // EventDataObjectDeserializer.getIntegrationApiVersion() and compared against the
    // event's own api_version field; a mismatch makes getObject() return empty and forces
    // the deserializeUnsafe() fallback in SubscriptionServiceImpl.deserializeEventObject().
    private static final String WEBHOOK_SECRET = "whsec_test_secret";
    private static final String MATCHING_API_VERSION = "2024-06-20";
    private static final String MISMATCHED_API_VERSION = "2025-01-01";

    private SubscriptionServiceImpl webhookConfiguredService() {
        StripeProperties props = new StripeProperties();
        props.setWebhookSecret(WEBHOOK_SECRET);
        return new SubscriptionServiceImpl(props, userRepository);
    }

    private String signedHeaderFor(String payload) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        String signedPayload = timestamp + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
        return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(hash);
    }

    private String subscriptionEventJson(String type, String customerId, String status,
                                         String apiVersion, Long currentPeriodEnd, Long trialEnd) {
        String cpe = currentPeriodEnd != null ? "\"current_period_end\": " + currentPeriodEnd + "," : "";
        String te = trialEnd != null ? "\"trial_end\": " + trialEnd + "," : "";
        return """
                {
                  "id": "evt_test",
                  "object": "event",
                  "api_version": "%s",
                  "type": "%s",
                  "data": {
                    "object": {
                      "id": "sub_test",
                      "object": "subscription",
                      "customer": "%s",
                      %s%s
                      "status": "%s"
                    }
                  }
                }
                """.formatted(apiVersion, type, customerId, cpe, te, status);
    }

    private String invoiceEventJson(String type, String customerId, String apiVersion, String subscriptionId) {
        String sub = subscriptionId != null ? "\"" + subscriptionId + "\"" : "null";
        return """
                {
                  "id": "evt_test",
                  "object": "event",
                  "api_version": "%s",
                  "type": "%s",
                  "data": {
                    "object": {
                      "id": "in_test",
                      "object": "invoice",
                      "customer": "%s",
                      "subscription": %s
                    }
                  }
                }
                """.formatted(apiVersion, type, customerId, sub);
    }

    private String genericEventJson(String type, String apiVersion) {
        return """
                {
                  "id": "evt_test",
                  "object": "event",
                  "api_version": "%s",
                  "type": "%s",
                  "data": { "object": { "object": "customer" } }
                }
                """.formatted(apiVersion, type);
    }

    @Test
    void processWebhookEvent_subscriptionCreatedTrialing_setsTrialStatusAndTimestamps() throws Exception {
        User user = userWithCustomerId("cus_1", SubscriptionStatus.NONE);
        when(userRepository.findByStripeCustomerId("cus_1")).thenReturn(Optional.of(user));
        String payload = subscriptionEventJson("customer.subscription.created", "cus_1", "trialing",
                MATCHING_API_VERSION, 2_000_000_000L, 1_999_999_999L);

        webhookConfiguredService().processWebhookEvent(payload, signedHeaderFor(payload));

        assertThat(user.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.TRIAL);
        assertThat(user.getStripeSubscriptionId()).isEqualTo("sub_test");
        assertThat(user.getTrialEndsAt()).isEqualTo(Instant.ofEpochSecond(1_999_999_999L));
        assertThat(user.getCurrentPeriodEndsAt()).isEqualTo(Instant.ofEpochSecond(2_000_000_000L));
        verify(userRepository).save(user);
    }

    @Test
    void processWebhookEvent_subscriptionUpdatedActive_setsActiveStatus() throws Exception {
        User user = userWithCustomerId("cus_2", SubscriptionStatus.TRIAL);
        when(userRepository.findByStripeCustomerId("cus_2")).thenReturn(Optional.of(user));
        String payload = subscriptionEventJson("customer.subscription.updated", "cus_2", "active",
                MATCHING_API_VERSION, 2_000_000_000L, null);

        webhookConfiguredService().processWebhookEvent(payload, signedHeaderFor(payload));

        assertThat(user.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(userRepository).save(user);
    }

    @Test
    void processWebhookEvent_subscriptionPastDue_setsLapsedStatus() throws Exception {
        User user = userWithCustomerId("cus_3", SubscriptionStatus.ACTIVE);
        when(userRepository.findByStripeCustomerId("cus_3")).thenReturn(Optional.of(user));
        String payload = subscriptionEventJson("customer.subscription.updated", "cus_3", "past_due",
                MATCHING_API_VERSION, null, null);

        webhookConfiguredService().processWebhookEvent(payload, signedHeaderFor(payload));

        assertThat(user.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.LAPSED);
    }

    @Test
    void processWebhookEvent_subscriptionDeleted_setsLapsedStatus() throws Exception {
        User user = userWithCustomerId("cus_4", SubscriptionStatus.ACTIVE);
        when(userRepository.findByStripeCustomerId("cus_4")).thenReturn(Optional.of(user));
        String payload = subscriptionEventJson("customer.subscription.deleted", "cus_4", "canceled",
                MATCHING_API_VERSION, null, null);

        webhookConfiguredService().processWebhookEvent(payload, signedHeaderFor(payload));

        assertThat(user.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.LAPSED);
        verify(userRepository).save(user);
    }

    @Test
    void processWebhookEvent_invoicePaymentSucceeded_nonTrialUser_setsActive() throws Exception {
        User user = userWithCustomerId("cus_5", SubscriptionStatus.NONE);
        when(userRepository.findByStripeCustomerId("cus_5")).thenReturn(Optional.of(user));
        String payload = invoiceEventJson("invoice.payment_succeeded", "cus_5", MATCHING_API_VERSION, "sub_1");

        webhookConfiguredService().processWebhookEvent(payload, signedHeaderFor(payload));

        assertThat(user.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(userRepository).save(user);
    }

    @Test
    void processWebhookEvent_invoicePaymentSucceeded_trialUser_leavesStatusUnchanged() throws Exception {
        User user = userWithCustomerId("cus_6", SubscriptionStatus.TRIAL);
        when(userRepository.findByStripeCustomerId("cus_6")).thenReturn(Optional.of(user));
        String payload = invoiceEventJson("invoice.payment_succeeded", "cus_6", MATCHING_API_VERSION, "sub_1");

        webhookConfiguredService().processWebhookEvent(payload, signedHeaderFor(payload));

        assertThat(user.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.TRIAL);
        verify(userRepository, never()).save(any());
    }

    @Test
    void processWebhookEvent_invoicePaymentSucceeded_noSubscriptionOnInvoice_noOp() throws Exception {
        String payload = invoiceEventJson("invoice.payment_succeeded", "cus_7", MATCHING_API_VERSION, null);

        webhookConfiguredService().processWebhookEvent(payload, signedHeaderFor(payload));

        verify(userRepository, never()).findByStripeCustomerId(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void processWebhookEvent_invoicePaymentFailed_setsLapsedStatus() throws Exception {
        User user = userWithCustomerId("cus_8", SubscriptionStatus.ACTIVE);
        when(userRepository.findByStripeCustomerId("cus_8")).thenReturn(Optional.of(user));
        String payload = invoiceEventJson("invoice.payment_failed", "cus_8", MATCHING_API_VERSION, "sub_1");

        webhookConfiguredService().processWebhookEvent(payload, signedHeaderFor(payload));

        assertThat(user.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.LAPSED);
        verify(userRepository).save(user);
    }

    @Test
    void processWebhookEvent_unknownCustomer_noOpsWithoutError() throws Exception {
        when(userRepository.findByStripeCustomerId("cus_missing")).thenReturn(Optional.empty());
        String payload = subscriptionEventJson("customer.subscription.updated", "cus_missing", "active",
                MATCHING_API_VERSION, null, null);

        webhookConfiguredService().processWebhookEvent(payload, signedHeaderFor(payload));

        verify(userRepository, never()).save(any());
    }

    @Test
    void processWebhookEvent_unhandledEventType_noOp() throws Exception {
        String payload = genericEventJson("customer.updated", MATCHING_API_VERSION);

        webhookConfiguredService().processWebhookEvent(payload, signedHeaderFor(payload));

        verify(userRepository, never()).findByStripeCustomerId(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void processWebhookEvent_invalidSignature_throwsIllegalArgument() {
        String payload = genericEventJson("customer.updated", MATCHING_API_VERSION);

        assertThatThrownBy(() ->
                webhookConfiguredService().processWebhookEvent(payload, "t=1,v1=deadbeef"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // Regression test for the live bug: account default API version didn't match the
    // SDK's pinned version, so getObject() came back empty and the event was never
    // processed at all (see deserializeEventObject's deserializeUnsafe() fallback).
    @Test
    void processWebhookEvent_apiVersionMismatch_stillUpdatesStatusViaFallback() throws Exception {
        User user = userWithCustomerId("cus_9", SubscriptionStatus.NONE);
        when(userRepository.findByStripeCustomerId("cus_9")).thenReturn(Optional.of(user));
        String payload = subscriptionEventJson("customer.subscription.updated", "cus_9", "active",
                MISMATCHED_API_VERSION, 2_000_000_000L, null);

        webhookConfiguredService().processWebhookEvent(payload, signedHeaderFor(payload));

        assertThat(user.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    // Regression test for the live bug: Subscription.getCurrentPeriodEnd() can come back
    // null (field relocated in newer API versions) and unboxing it threw a
    // NullPointerException that aborted the whole subscription status update.
    @Test
    void processWebhookEvent_missingCurrentPeriodEnd_doesNotThrowAndLeavesFieldUnset() throws Exception {
        User user = userWithCustomerId("cus_10", SubscriptionStatus.NONE);
        when(userRepository.findByStripeCustomerId("cus_10")).thenReturn(Optional.of(user));
        String payload = subscriptionEventJson("customer.subscription.updated", "cus_10", "active",
                MATCHING_API_VERSION, null, null);

        webhookConfiguredService().processWebhookEvent(payload, signedHeaderFor(payload));

        assertThat(user.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(user.getCurrentPeriodEndsAt()).isNull();
    }

    @Test
    void processWebhookEvent_trialingMissingTrialEnd_doesNotThrowAndLeavesFieldUnset() throws Exception {
        User user = userWithCustomerId("cus_11", SubscriptionStatus.NONE);
        when(userRepository.findByStripeCustomerId("cus_11")).thenReturn(Optional.of(user));
        String payload = subscriptionEventJson("customer.subscription.created", "cus_11", "trialing",
                MATCHING_API_VERSION, null, null);

        webhookConfiguredService().processWebhookEvent(payload, signedHeaderFor(payload));

        assertThat(user.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.TRIAL);
        assertThat(user.getTrialEndsAt()).isNull();
    }

    // --- Helpers ---

    private User userWithStatus(SubscriptionStatus status) {
        return User.builder()
                .email("test@example.com")
                .subscriptionStatus(status)
                .build();
    }

    private User userWithCustomerId(String stripeCustomerId, SubscriptionStatus status) {
        return User.builder()
                .email("test@example.com")
                .stripeCustomerId(stripeCustomerId)
                .subscriptionStatus(status)
                .build();
    }
}
