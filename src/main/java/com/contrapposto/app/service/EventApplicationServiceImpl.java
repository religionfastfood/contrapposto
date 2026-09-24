package com.contrapposto.app.service;

import com.contrapposto.app.event.ApplicationDecidedEvent;
import com.contrapposto.app.event.ApplicationSubmittedEvent;
import com.contrapposto.app.model.ApplicationInitiator;
import com.contrapposto.app.model.ApplicationStatus;
import com.contrapposto.app.model.Event;
import com.contrapposto.app.model.EventApplication;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.User;
import com.contrapposto.app.model.ModelProfile;
import com.contrapposto.app.repository.EventApplicationRepository;
import com.contrapposto.app.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final ModelProfileService modelProfileService;
    private final ApplicationEventPublisher eventPublisher;

    public EventApplicationServiceImpl(EventApplicationRepository eventApplicationRepository, EventService eventService,
                                        UserRepository userRepository, SubscriptionService subscriptionService,
                                        ModelProfileService modelProfileService, ApplicationEventPublisher eventPublisher) {
        this.eventApplicationRepository = eventApplicationRepository;
        this.eventService = eventService;
        this.userRepository = userRepository;
        this.subscriptionService = subscriptionService;
        this.modelProfileService = modelProfileService;
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
        return findRespondable(actor, applicationId).map(application -> {
            requireEventNotAlreadyAssigned(application);
            return decide(application, ApplicationStatus.ACCEPTED);
        });
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

    @Override
    public Optional<AssignedModelView> findAssignedModel(Event event) {
        return eventApplicationRepository.findByEventAndStatus(event, ApplicationStatus.ACCEPTED)
                .map(application -> {
                    User model = application.getModel();
                    Optional<ModelProfile> profile = modelProfileService.findByUser(model);
                    String firstName = profile.map(ModelProfile::getDisplayName)
                            .filter(StringUtils::hasText)
                            .map(EventApplicationServiceImpl::firstNameOf)
                            .orElse(null);
                    String photoUrl = profile.map(ModelProfile::getPhotoUrls)
                            .filter(urls -> !urls.isEmpty())
                            .map(urls -> urls.get(0))
                            .orElse(null);
                    return new AssignedModelView(firstName, photoUrl);
                });
    }

    @Override
    public Map<Long, AssignedModelView> findAssignedModels(List<Event> events) {
        Map<Long, AssignedModelView> result = new HashMap<>();
        for (Event event : events) {
            findAssignedModel(event).ifPresent(view -> result.put(event.getId(), view));
        }
        return result;
    }

    private static String firstNameOf(String displayName) {
        String trimmed = displayName.trim();
        int spaceIndex = trimmed.indexOf(' ');
        return spaceIndex == -1 ? trimmed : trimmed.substring(0, spaceIndex);
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

    // An event has at most one assigned model. This only guards the ACCEPTED transition itself --
    // an event can still have several PENDING applications/invitations in flight; whichever is
    // accepted first wins, and any other accept attempt after that fails here. (`application` is
    // always still PENDING at this point per findRespondable's filter, so it's never the row this
    // check finds.)
    private void requireEventNotAlreadyAssigned(EventApplication application) {
        if (eventApplicationRepository.findByEventAndStatus(application.getEvent(), ApplicationStatus.ACCEPTED).isPresent()) {
            throw new IllegalStateException("This event already has an assigned model");
        }
    }
}
