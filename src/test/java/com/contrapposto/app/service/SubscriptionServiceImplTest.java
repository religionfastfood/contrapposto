package com.contrapposto.app.service;

import com.contrapposto.app.config.StripeProperties;
import com.contrapposto.app.model.BillingPeriod;
import com.contrapposto.app.model.SubscriptionStatus;
import com.contrapposto.app.model.User;
import com.contrapposto.app.repository.UserRepository;
import net.jqwik.api.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    // --- Helpers ---

    private User userWithStatus(SubscriptionStatus status) {
        return User.builder()
                .email("test@example.com")
                .subscriptionStatus(status)
                .build();
    }
}
