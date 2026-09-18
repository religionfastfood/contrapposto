package com.contrapposto.app.service;

import com.contrapposto.app.model.ModelProfile;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.SubscriptionStatus;
import com.contrapposto.app.model.User;
import com.contrapposto.app.repository.ModelProfileRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModelSearchServiceImplTest {

    private final ModelProfileRepository modelProfileRepository = Mockito.mock(ModelProfileRepository.class);
    private final ModelSearchServiceImpl service = new ModelSearchServiceImpl(modelProfileRepository);

    // --- search ---

    @Test
    void search_blankCity_queriesWithoutCityFilter() {
        when(modelProfileRepository.findByUser_SubscriptionStatusIn(SubscriptionStatus.active()))
                .thenReturn(List.of());

        service.search(null);
        service.search("");
        service.search("   ");

        verify(modelProfileRepository, org.mockito.Mockito.times(3))
                .findByUser_SubscriptionStatusIn(SubscriptionStatus.active());
        verify(modelProfileRepository, never()).findByUser_SubscriptionStatusInAndCityIgnoreCase(any(), any());
    }

    @Test
    void search_withCity_queriesWithTrimmedCityFilter() {
        when(modelProfileRepository.findByUser_SubscriptionStatusInAndCityIgnoreCase(
                eq(SubscriptionStatus.active()), eq("Portland"))).thenReturn(List.of());

        service.search("  Portland  ");

        verify(modelProfileRepository).findByUser_SubscriptionStatusInAndCityIgnoreCase(
                SubscriptionStatus.active(), "Portland");
    }

    // --- findVisibleProfile ---

    @Test
    void findVisibleProfile_noSuchProfile_returnsEmpty() {
        when(modelProfileRepository.findById(1L)).thenReturn(Optional.empty());

        assertThat(service.findVisibleProfile(1L)).isEmpty();
    }

    @Property
    void findVisibleProfile_activeStatuses_returnsProfile(@ForAll("activeStatuses") SubscriptionStatus status) {
        ModelProfile profile = new ModelProfile(userWithStatus(status));
        when(modelProfileRepository.findById(5L)).thenReturn(Optional.of(profile));

        assertThat(service.findVisibleProfile(5L)).contains(profile);
    }

    @Property
    void findVisibleProfile_inactiveStatuses_returnsEmpty(@ForAll("inactiveStatuses") SubscriptionStatus status) {
        ModelProfile profile = new ModelProfile(userWithStatus(status));
        when(modelProfileRepository.findById(5L)).thenReturn(Optional.of(profile));

        assertThat(service.findVisibleProfile(5L)).isEmpty();
    }

    @Provide
    Arbitrary<SubscriptionStatus> activeStatuses() {
        return Arbitraries.of(SubscriptionStatus.TRIAL, SubscriptionStatus.ACTIVE);
    }

    @Provide
    Arbitrary<SubscriptionStatus> inactiveStatuses() {
        return Arbitraries.of(SubscriptionStatus.LAPSED, SubscriptionStatus.NONE);
    }

    private User userWithStatus(SubscriptionStatus status) {
        return User.builder().id(2L).email("model@example.com").role(Role.MODEL).subscriptionStatus(status).build();
    }
}
