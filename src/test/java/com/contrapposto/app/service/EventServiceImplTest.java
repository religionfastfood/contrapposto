package com.contrapposto.app.service;

import com.contrapposto.app.dto.EventRequest;
import com.contrapposto.app.model.Event;
import com.contrapposto.app.model.EventType;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.SubscriptionStatus;
import com.contrapposto.app.model.User;
import com.contrapposto.app.repository.EventRepository;
import com.contrapposto.app.repository.EventTypeRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.LongRange;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventServiceImplTest {

    private final EventRepository eventRepository = Mockito.mock(EventRepository.class);
    private final EventTypeRepository eventTypeRepository = Mockito.mock(EventTypeRepository.class);
    private final SubscriptionService subscriptionService = Mockito.mock(SubscriptionService.class);
    private final EventServiceImpl service =
            new EventServiceImpl(eventRepository, eventTypeRepository, subscriptionService);

    {
        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private User organizer(long id) {
        return User.builder().id(id).email("organizer" + id + "@example.com").role(Role.ORGANIZER).build();
    }

    private EventRequest validRequest() {
        EventRequest request = new EventRequest();
        request.setEventTypeId(1L);
        request.setTitle("Gesture Night");
        request.setCity("Portland");
        request.setLocation("123 Main St");
        request.setStartTime(LocalDateTime.now().plusDays(1));
        return request;
    }

    private Event eventOwnedBy(User owner) {
        Event event = new Event(owner);
        event.setEventType(new EventType("Gesture"));
        return event;
    }

    // --- create: subscription gating ---

    @Test
    void create_activeSubscription_savesEvent() {
        User organizer = organizer(1L);
        when(subscriptionService.isSubscriptionActive(organizer)).thenReturn(true);
        when(eventTypeRepository.findById(1L)).thenReturn(Optional.of(new EventType("Gesture")));

        Event event = service.create(organizer, validRequest());

        assertThat(event.getOrganizer()).isEqualTo(organizer);
        assertThat(event.getTitle()).isEqualTo("Gesture Night");
        verify(eventRepository).save(any());
    }

    @Test
    void create_inactiveSubscription_throwsAndDoesNotSave() {
        User organizer = organizer(1L);
        when(subscriptionService.isSubscriptionActive(organizer)).thenReturn(false);

        assertThatThrownBy(() -> service.create(organizer, validRequest()))
                .isInstanceOf(IllegalStateException.class);
        verify(eventRepository, never()).save(any());
    }

    @Property
    void create_alwaysGatedBySubscriptionActive(@ForAll("statuses") SubscriptionStatus status) {
        EventRepository repo = Mockito.mock(EventRepository.class);
        EventTypeRepository typeRepo = Mockito.mock(EventTypeRepository.class);
        SubscriptionService realSubscriptionService = new SubscriptionServiceImpl(
                new com.contrapposto.app.config.StripeProperties(), Mockito.mock(com.contrapposto.app.repository.UserRepository.class));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(typeRepo.findById(1L)).thenReturn(Optional.of(new EventType("Gesture")));
        EventServiceImpl svc = new EventServiceImpl(repo, typeRepo, realSubscriptionService);

        User user = User.builder().id(1L).email("o@example.com").role(Role.ORGANIZER)
                .subscriptionStatus(status).build();

        if (status.isActive()) {
            assertThat(svc.create(user, validRequest())).isNotNull();
        } else {
            assertThatThrownBy(() -> svc.create(user, validRequest())).isInstanceOf(IllegalStateException.class);
        }
    }

    @Provide
    Arbitrary<SubscriptionStatus> statuses() {
        return Arbitraries.of(SubscriptionStatus.values());
    }

    // --- update/delete: ownership ---

    @Test
    void update_ownEvent_appliesChangesAndSaves() {
        User organizer = organizer(1L);
        Event existing = eventOwnedBy(organizer);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(eventTypeRepository.findById(1L)).thenReturn(Optional.of(new EventType("Gesture")));
        EventRequest request = validRequest();
        request.setTitle("Updated Title");

        Optional<Event> result = service.update(organizer, 10L, request);

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Updated Title");
    }

    @Test
    void update_notOwner_returnsEmptyAndDoesNotSave() {
        User owner = organizer(1L);
        User attacker = organizer(2L);
        Event existing = eventOwnedBy(owner);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(existing));

        Optional<Event> result = service.update(attacker, 10L, validRequest());

        assertThat(result).isEmpty();
        verify(eventRepository, never()).save(any());
    }

    @Test
    void update_nonExistentEvent_returnsEmpty() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThat(service.update(organizer(1L), 999L, validRequest())).isEmpty();
    }

    @Test
    void delete_ownEvent_deletesAndReturnsTrue() {
        User organizer = organizer(1L);
        Event existing = eventOwnedBy(organizer);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(existing));

        boolean deleted = service.delete(organizer, 10L);

        assertThat(deleted).isTrue();
        verify(eventRepository).delete(existing);
    }

    @Test
    void delete_notOwner_returnsFalseAndDoesNotDelete() {
        User owner = organizer(1L);
        User attacker = organizer(2L);
        Event existing = eventOwnedBy(owner);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(existing));

        boolean deleted = service.delete(attacker, 10L);

        assertThat(deleted).isFalse();
        verify(eventRepository, never()).delete(any());
    }

    @Property
    void ownershipCheck_neverSucceedsForADifferentOrganizerId(
            @ForAll @LongRange(min = 1, max = 1000) long ownerId,
            @ForAll @LongRange(min = 1, max = 1000) long requesterId) {
        Assume.that(ownerId != requesterId);

        EventRepository repo = Mockito.mock(EventRepository.class);
        EventTypeRepository typeRepo = Mockito.mock(EventTypeRepository.class);
        SubscriptionService subService = Mockito.mock(SubscriptionService.class);
        EventServiceImpl svc = new EventServiceImpl(repo, typeRepo, subService);

        User owner = organizer(ownerId);
        User requester = organizer(requesterId);
        Event event = eventOwnedBy(owner);
        when(repo.findById(10L)).thenReturn(Optional.of(event));

        assertThat(svc.findOwnedById(requester, 10L)).isEmpty();
        assertThat(svc.update(requester, 10L, validRequest())).isEmpty();
        assertThat(svc.delete(requester, 10L)).isFalse();
    }

    // --- read paths ---

    @Test
    void upcomingByCity_delegatesToRepository() {
        User organizer = organizer(1L);
        Event event = eventOwnedBy(organizer);
        when(eventRepository.findByCityIgnoreCaseAndStartTimeAfterOrderByStartTimeAsc(eq("Portland"), any()))
                .thenReturn(java.util.List.of(event));

        assertThat(service.upcomingByCity("Portland")).containsExactly(event);
    }
}
