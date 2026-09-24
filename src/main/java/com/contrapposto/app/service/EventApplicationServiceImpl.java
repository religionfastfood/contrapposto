package com.contrapposto.app.service;

import com.contrapposto.app.event.ApplicationDecidedEvent;
import com.contrapposto.app.event.ApplicationSubmittedEvent;
import com.contrapposto.app.model.ApplicationInitiator;
import com.contrapposto.app.model.ApplicationStatus;
import com.contrapposto.app.model.Event;
import com.contrapposto.app.model.EventApplication;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.User;
import com.contrapposto.app.repository.EventApplicationRepository;
import com.contrapposto.app.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

// @Transactional is required here (not just on the repository methods it calls): the event
// publishers below rely on @TransactionalEventListener(AFTER_COMMIT) in ApplicationNotificationListener,
// which only fires if a transaction is still active when publishEvent() is called. Without a
// class-level transaction, each repository call opens and commits its own short transaction, so by
// the time publishEvent() runs there is nothing left for AFTER_COMMIT to attach to and the event is
// silently dropped.
@Service
@Transactional
public class EventApplicationServiceImpl implements EventApplicationService {

    private static final List<ApplicationStatus> ACTIVE_STATUSES = List.of(ApplicationStatus.PENDING, ApplicationStatus.ACCEPTED);

    private final EventApplicationRepository eventApplicationRepository;
    private final EventService eventService;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    private final ApplicationEventPublisher eventPublisher;

    public EventApplicationServiceImpl(EventApplicationRepository eventApplicationRepository, EventService eventService,
                                        UserRepository userRepository, SubscriptionService subscriptionService,
                                        ApplicationEventPublisher eventPublisher) {
        this.eventApplicationRepository = eventApplicationRepository;
        this.eventService = eventService;
        this.userRepository = userRepository;
        this.subscriptionService = subscriptionService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public EventApplication apply(User model, Long eventId, String message) {
        Event event = eventService.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found"));
        requireActiveModel(model);
        requireNoActiveRow(event, model);
        EventApplication application = new EventApplication(event, model, ApplicationInitiator.MODEL, message);
        return save(application);
    }

    @Override
    public EventApplication invite(User organizer, Long eventId, Long modelId, String message) {
        Event event = eventService.findOwnedById(organizer, eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found"));
        User model = userRepository.findById(modelId)
                .orElseThrow(() -> new NoSuchElementException("Model not found"));
        if (model.getRole() != Role.MODEL) {
            throw new IllegalStateException("Invitations can only be sent to models");
        }
        requireActiveModel(model);
        requireNoActiveRow(event, model);
        EventApplication application = new EventApplication(event, model, ApplicationInitiator.ORGANIZER, message);
        return save(application);
    }

    @Override
    public Optional<EventApplication> accept(User actor, Long applicationId) {
        return findRespondable(actor, applicationId).map(application -> decide(application, ApplicationStatus.ACCEPTED));
    }

    @Override
    public Optional<EventApplication> decline(User actor, Long applicationId) {
        return findRespondable(actor, applicationId).map(application -> decide(application, ApplicationStatus.DECLINED));
    }

    @Override
    public Optional<EventApplication> withdraw(User actor, Long applicationId) {
        return eventApplicationRepository.findById(applicationId)
                .filter(application -> application.getStatus() == ApplicationStatus.PENDING && isInitiator(actor, application))
                .map(application -> {
                    application.setStatus(ApplicationStatus.WITHDRAWN);
                    application.setRespondedAt(LocalDateTime.now());
                    return eventApplicationRepository.save(application);
                });
    }

    @Override
    public List<EventApplication> findForModel(User model) {
        return eventApplicationRepository.findByModelOrderByCreatedAtDesc(model);
    }

    @Override
    public List<EventApplication> findForOrganizer(User organizer) {
        return eventApplicationRepository.findByEvent_OrganizerOrderByCreatedAtDesc(organizer);
    }

    @Override
    public Optional<EventApplication> findActiveForEventAndModel(Event event, User model) {
        return eventApplicationRepository.findByEventAndModelAndStatusIn(event, model, ACTIVE_STATUSES);
    }

    @Override
    public boolean hasPriorDecline(Event event, User model) {
        return eventApplicationRepository.existsByEventAndModelAndStatus(event, model, ApplicationStatus.DECLINED);
    }

    private EventApplication save(EventApplication application) {
        EventApplication saved = eventApplicationRepository.save(application);
        eventPublisher.publishEvent(new ApplicationSubmittedEvent(saved.getId()));
        return saved;
    }

    private EventApplication decide(EventApplication application, ApplicationStatus decision) {
        application.setStatus(decision);
        application.setRespondedAt(LocalDateTime.now());
        EventApplication saved = eventApplicationRepository.save(application);
        eventPublisher.publishEvent(new ApplicationDecidedEvent(saved.getId()));
        return saved;
    }

    private Optional<EventApplication> findRespondable(User actor, Long applicationId) {
        return eventApplicationRepository.findById(applicationId)
                .filter(application -> application.getStatus() == ApplicationStatus.PENDING && isResponder(actor, application));
    }

    // The responder is the party that didn't initiate the row -- whoever needs to accept/decline it.
    private boolean isResponder(User actor, EventApplication application) {
        return application.getInitiatedBy() == ApplicationInitiator.MODEL
                ? application.getEvent().getOrganizer().getId().equals(actor.getId())
                : application.getModel().getId().equals(actor.getId());
    }

    // The initiator is the party that created the row -- the only one allowed to withdraw it.
    private boolean isInitiator(User actor, EventApplication application) {
        return application.getInitiatedBy() == ApplicationInitiator.MODEL
                ? application.getModel().getId().equals(actor.getId())
                : application.getEvent().getOrganizer().getId().equals(actor.getId());
    }

    private void requireActiveModel(User model) {
        if (!subscriptionService.isSubscriptionActive(model)) {
            throw new IllegalStateException("An active subscription is required for this model to apply to or be invited to events");
        }
    }

    private void requireNoActiveRow(Event event, User model) {
        if (eventApplicationRepository.existsByEventAndModelAndStatusIn(event, model, ACTIVE_STATUSES)) {
            throw new IllegalStateException("An active application or invitation already exists for this model and event");
        }
    }
}
