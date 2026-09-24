package com.contrapposto.app.service;

import com.contrapposto.app.event.ApplicationDecidedEvent;
import com.contrapposto.app.event.ApplicationSubmittedEvent;
import com.contrapposto.app.model.ApplicationInitiator;
import com.contrapposto.app.model.ApplicationStatus;
import com.contrapposto.app.model.Event;
import com.contrapposto.app.model.EventApplication;
import com.contrapposto.app.model.EventType;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.User;
import com.contrapposto.app.repository.EventApplicationRepository;
import com.contrapposto.app.repository.UserRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventApplicationServiceImplTest {

    private final EventApplicationRepository eventApplicationRepository = Mockito.mock(EventApplicationRepository.class);
    private final EventService eventService = Mockito.mock(EventService.class);
    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final SubscriptionService subscriptionService = Mockito.mock(SubscriptionService.class);
    private final ModelProfileService modelProfileService = Mockito.mock(ModelProfileService.class);
    private final ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
    private final EventApplicationServiceImpl service = new EventApplicationServiceImpl(
            eventApplicationRepository, eventService, userRepository, subscriptionService, modelProfileService, eventPublisher);

    {
        when(eventApplicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private User model(long id) {
        return User.builder().id(id).email("model" + id + "@example.com").role(Role.MODEL).build();
    }

    private User organizer(long id) {
        return User.builder().id(id).email("organizer" + id + "@example.com").role(Role.ORGANIZER).build();
    }

    private Event event(User organizer) {
        Event event = new Event(organizer);
        event.setId(10L);
        event.setEventType(new EventType("Gesture"));
        event.setTitle("Gesture Night");
        return event;
    }

    // --- apply ---

    @Test
    void apply_activeModelNoActiveRow_savesAndPublishesSubmitted() {
        User organizer = organizer(1L);
        User model = model(2L);
        Event event = event(organizer);
        when(eventService.findById(10L)).thenReturn(Optional.of(event));
        when(subscriptionService.isSubscriptionActive(model)).thenReturn(true);
        when(eventApplicationRepository.existsByEventAndModelAndStatusIn(eq(event), eq(model), any())).thenReturn(false);

        EventApplication result = service.apply(model, 10L, "Excited to model!");

        assertThat(result.getInitiatedBy()).isEqualTo(ApplicationInitiator.MODEL);
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        verify(eventPublisher).publishEvent(any(ApplicationSubmittedEvent.class));
    }

    @Test
    void apply_inactiveSubscription_throwsAndDoesNotSave() {
        User organizer = organizer(1L);
        User model = model(2L);
        when(eventService.findById(10L)).thenReturn(Optional.of(event(organizer)));
        when(subscriptionService.isSubscriptionActive(model)).thenReturn(false);

        assertThatThrownBy(() -> service.apply(model, 10L, null)).isInstanceOf(IllegalStateException.class);
        verify(eventApplicationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void apply_activeRowAlreadyExists_throwsAndDoesNotSave() {
        User organizer = organizer(1L);
        User model = model(2L);
        Event event = event(organizer);
        when(eventService.findById(10L)).thenReturn(Optional.of(event));
        when(subscriptionService.isSubscriptionActive(model)).thenReturn(true);
        when(eventApplicationRepository.existsByEventAndModelAndStatusIn(eq(event), eq(model), any())).thenReturn(true);

        assertThatThrownBy(() -> service.apply(model, 10L, null)).isInstanceOf(IllegalStateException.class);
        verify(eventApplicationRepository, never()).save(any());
    }

    @Property
    void apply_succeedsIffNoPriorActiveRow(@ForAll("statuses") ApplicationStatus priorStatus) {
        EventApplicationRepository repo = Mockito.mock(EventApplicationRepository.class);
        EventService evtService = Mockito.mock(EventService.class);
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        SubscriptionService subService = Mockito.mock(SubscriptionService.class);
        ModelProfileService profileService = Mockito.mock(ModelProfileService.class);
        ApplicationEventPublisher publisher = Mockito.mock(ApplicationEventPublisher.class);
        EventApplicationServiceImpl svc = new EventApplicationServiceImpl(repo, evtService, userRepo, subService, profileService, publisher);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User organizer = organizer(1L);
        User model = model(2L);
        Event event = event(organizer);
        when(evtService.findById(10L)).thenReturn(Optional.of(event));
        when(subService.isSubscriptionActive(model)).thenReturn(true);

        boolean priorIsActive = priorStatus == ApplicationStatus.PENDING || priorStatus == ApplicationStatus.ACCEPTED;
        when(repo.existsByEventAndModelAndStatusIn(eq(event), eq(model), any())).thenReturn(priorIsActive);

        if (priorIsActive) {
            assertThatThrownBy(() -> svc.apply(model, 10L, null)).isInstanceOf(IllegalStateException.class);
        } else {
            assertThat(svc.apply(model, 10L, null)).isNotNull();
        }
    }

    @Provide
    Arbitrary<ApplicationStatus> statuses() {
        return Arbitraries.of(ApplicationStatus.values());
    }

    // --- invite ---

    @Test
    void invite_ownedEventAndActiveModel_savesAndPublishesSubmitted() {
        User organizer = organizer(1L);
        User model = model(2L);
        Event event = event(organizer);
        when(eventService.findOwnedById(organizer, 10L)).thenReturn(Optional.of(event));
        when(userRepository.findById(2L)).thenReturn(Optional.of(model));
        when(subscriptionService.isSubscriptionActive(model)).thenReturn(true);

        EventApplication result = service.invite(organizer, 10L, 2L, "Would love to have you");

        assertThat(result.getInitiatedBy()).isEqualTo(ApplicationInitiator.ORGANIZER);
        verify(eventPublisher).publishEvent(any(ApplicationSubmittedEvent.class));
    }

    @Test
    void invite_unownedEvent_throwsNoSuchElement() {
        User organizer = organizer(1L);
        when(eventService.findOwnedById(organizer, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.invite(organizer, 10L, 2L, null))
                .isInstanceOf(java.util.NoSuchElementException.class);
        verify(eventApplicationRepository, never()).save(any());
    }

    @Test
    void invite_targetIsNotAModel_throwsIllegalState() {
        User organizer = organizer(1L);
        Event event = event(organizer);
        User notAModel = organizer(3L);
        when(eventService.findOwnedById(organizer, 10L)).thenReturn(Optional.of(event));
        when(userRepository.findById(3L)).thenReturn(Optional.of(notAModel));

        assertThatThrownBy(() -> service.invite(organizer, 10L, 3L, null)).isInstanceOf(IllegalStateException.class);
        verify(eventApplicationRepository, never()).save(any());
    }

    // --- accept / decline ---

    @Test
    void accept_modelInitiated_correctResponderIsOrganizer_succeeds() {
        User organizer = organizer(1L);
        User model = model(2L);
        EventApplication application = new EventApplication(event(organizer), model, ApplicationInitiator.MODEL, null);
        application.setId(50L);
        when(eventApplicationRepository.findById(50L)).thenReturn(Optional.of(application));

        Optional<EventApplication> result = service.accept(organizer, 50L);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        assertThat(result.get().getRespondedAt()).isNotNull();
        verify(eventPublisher).publishEvent(any(ApplicationDecidedEvent.class));
    }

    @Test
    void accept_modelInitiated_wrongResponder_returnsEmpty() {
        User organizer = organizer(1L);
        User model = model(2L);
        User someoneElse = organizer(3L);
        EventApplication application = new EventApplication(event(organizer), model, ApplicationInitiator.MODEL, null);
        application.setId(50L);
        when(eventApplicationRepository.findById(50L)).thenReturn(Optional.of(application));

        assertThat(service.accept(someoneElse, 50L)).isEmpty();
        verify(eventApplicationRepository, never()).save(any());
    }

    @Test
    void accept_organizerInitiated_correctResponderIsModel_succeeds() {
        User organizer = organizer(1L);
        User model = model(2L);
        EventApplication application = new EventApplication(event(organizer), model, ApplicationInitiator.ORGANIZER, null);
        application.setId(51L);
        when(eventApplicationRepository.findById(51L)).thenReturn(Optional.of(application));

        assertThat(service.accept(model, 51L)).isPresent();
    }

    @Test
    void accept_eventAlreadyHasAcceptedModel_throwsAndDoesNotSave() {
        User organizer = organizer(1L);
        User model = model(2L);
        Event event = event(organizer);
        EventApplication application = new EventApplication(event, model, ApplicationInitiator.MODEL, null);
        application.setId(53L);
        when(eventApplicationRepository.findById(53L)).thenReturn(Optional.of(application));

        User alreadyAcceptedModel = model(3L);
        EventApplication alreadyAccepted = new EventApplication(event, alreadyAcceptedModel, ApplicationInitiator.MODEL, null);
        alreadyAccepted.setId(54L);
        alreadyAccepted.setStatus(ApplicationStatus.ACCEPTED);
        when(eventApplicationRepository.findByEventAndStatus(event, ApplicationStatus.ACCEPTED))
                .thenReturn(Optional.of(alreadyAccepted));

        assertThatThrownBy(() -> service.accept(organizer, 53L)).isInstanceOf(IllegalStateException.class);
        verify(eventApplicationRepository, never()).save(application);
    }

    @Test
    void decline_alreadyDecided_returnsEmpty() {
        User organizer = organizer(1L);
        User model = model(2L);
        EventApplication application = new EventApplication(event(organizer), model, ApplicationInitiator.MODEL, null);
        application.setId(52L);
        application.setStatus(ApplicationStatus.ACCEPTED);
        when(eventApplicationRepository.findById(52L)).thenReturn(Optional.of(application));

        assertThat(service.decline(organizer, 52L)).isEmpty();
    }

    // --- withdraw ---

    @Test
    void withdraw_byOriginatingModel_succeeds() {
        User organizer = organizer(1L);
        User model = model(2L);
        EventApplication application = new EventApplication(event(organizer), model, ApplicationInitiator.MODEL, null);
        application.setId(60L);
        when(eventApplicationRepository.findById(60L)).thenReturn(Optional.of(application));

        Optional<EventApplication> result = service.withdraw(model, 60L);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
    }

    @Test
    void withdraw_byResponderNotInitiator_returnsEmpty() {
        User organizer = organizer(1L);
        User model = model(2L);
        EventApplication application = new EventApplication(event(organizer), model, ApplicationInitiator.MODEL, null);
        application.setId(61L);
        when(eventApplicationRepository.findById(61L)).thenReturn(Optional.of(application));

        assertThat(service.withdraw(organizer, 61L)).isEmpty();
    }

    // --- hasPriorDecline ---

    @Test
    void hasPriorDecline_delegatesToRepository() {
        User organizer = organizer(1L);
        User model = model(2L);
        Event event = event(organizer);
        when(eventApplicationRepository.existsByEventAndModelAndStatus(event, model, ApplicationStatus.DECLINED)).thenReturn(true);

        assertThat(service.hasPriorDecline(event, model)).isTrue();
    }

    // --- findAssignedModel / findAssignedModels ---

    @Test
    void findAssignedModel_noAcceptedApplication_returnsEmpty() {
        Event event = event(organizer(1L));
        when(eventApplicationRepository.findByEventAndStatus(event, ApplicationStatus.ACCEPTED)).thenReturn(Optional.empty());

        assertThat(service.findAssignedModel(event)).isEmpty();
    }

    @Test
    void findAssignedModel_acceptedWithProfile_returnsFirstNameAndPrimaryPhoto() {
        User organizer = organizer(1L);
        User model = model(2L);
        Event event = event(organizer);
        EventApplication accepted = new EventApplication(event, model, ApplicationInitiator.MODEL, null);
        accepted.setStatus(ApplicationStatus.ACCEPTED);
        when(eventApplicationRepository.findByEventAndStatus(event, ApplicationStatus.ACCEPTED)).thenReturn(Optional.of(accepted));

        com.contrapposto.app.model.ModelProfile profile = new com.contrapposto.app.model.ModelProfile(model);
        profile.setDisplayName("Ava Chen");
        profile.getPhotoUrls().add("https://example.com/ava-1.jpg");
        profile.getPhotoUrls().add("https://example.com/ava-2.jpg");
        when(modelProfileService.findByUser(model)).thenReturn(Optional.of(profile));

        Optional<AssignedModelView> result = service.findAssignedModel(event);

        assertThat(result).isPresent();
        assertThat(result.get().firstName()).isEqualTo("Ava");
        assertThat(result.get().photoUrl()).isEqualTo("https://example.com/ava-1.jpg");
    }

    @Test
    void findAssignedModel_acceptedButNoProfile_returnsNullFirstNameAndPhoto() {
        User organizer = organizer(1L);
        User model = model(2L);
        Event event = event(organizer);
        EventApplication accepted = new EventApplication(event, model, ApplicationInitiator.MODEL, null);
        accepted.setStatus(ApplicationStatus.ACCEPTED);
        when(eventApplicationRepository.findByEventAndStatus(event, ApplicationStatus.ACCEPTED)).thenReturn(Optional.of(accepted));
        when(modelProfileService.findByUser(model)).thenReturn(Optional.empty());

        Optional<AssignedModelView> result = service.findAssignedModel(event);

        assertThat(result).isPresent();
        assertThat(result.get().firstName()).isNull();
        assertThat(result.get().photoUrl()).isNull();
    }

    @Test
    void findAssignedModels_batchesAcrossEvents_onlyIncludingAssignedOnes() {
        User organizer = organizer(1L);
        Event assignedEvent = event(organizer);
        assignedEvent.setId(20L);
        Event unassignedEvent = event(organizer);
        unassignedEvent.setId(21L);

        User model = model(2L);
        EventApplication accepted = new EventApplication(assignedEvent, model, ApplicationInitiator.MODEL, null);
        accepted.setStatus(ApplicationStatus.ACCEPTED);
        when(eventApplicationRepository.findByEventAndStatus(assignedEvent, ApplicationStatus.ACCEPTED)).thenReturn(Optional.of(accepted));
        when(eventApplicationRepository.findByEventAndStatus(unassignedEvent, ApplicationStatus.ACCEPTED)).thenReturn(Optional.empty());
        when(modelProfileService.findByUser(model)).thenReturn(Optional.empty());

        java.util.Map<Long, AssignedModelView> result = service.findAssignedModels(List.of(assignedEvent, unassignedEvent));

        assertThat(result).containsOnlyKeys(20L);
    }
}
