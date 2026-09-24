package com.contrapposto.app.service;

import com.contrapposto.app.event.ApplicationDecidedEvent;
import com.contrapposto.app.event.ApplicationSubmittedEvent;
import com.contrapposto.app.model.ApplicationInitiator;
import com.contrapposto.app.model.EventApplication;
import com.contrapposto.app.model.User;
import com.contrapposto.app.repository.EventApplicationRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Reacts to application/invitation lifecycle events after the originating transaction commits,
// so a notification is never sent for a change that ends up rolling back. Runs off the request
// thread (@Async) so a slow/failing notification can't fail or delay the triggering request.
// Re-fetches the EventApplication (rather than receiving it on the event) in its own read-only
// transaction, since the entity's lazy associations can't be touched once the publishing
// transaction's session has closed.
@Component
public class ApplicationNotificationListener {

    private final EventApplicationRepository eventApplicationRepository;
    private final NotificationService notificationService;

    public ApplicationNotificationListener(EventApplicationRepository eventApplicationRepository,
                                            NotificationService notificationService) {
        this.eventApplicationRepository = eventApplicationRepository;
        this.notificationService = notificationService;
    }

    @Async
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmitted(ApplicationSubmittedEvent event) {
        eventApplicationRepository.findById(event.applicationId()).ifPresent(application ->
                notificationService.notifySubmitted(application, responderTo(application)));
    }

    @Async
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDecided(ApplicationDecidedEvent event) {
        eventApplicationRepository.findById(event.applicationId()).ifPresent(application ->
                notificationService.notifyDecided(application, initiatorOf(application)));
    }

    private User responderTo(EventApplication application) {
        return application.getInitiatedBy() == ApplicationInitiator.MODEL
                ? application.getEvent().getOrganizer()
                : application.getModel();
    }

    private User initiatorOf(EventApplication application) {
        return application.getInitiatedBy() == ApplicationInitiator.MODEL
                ? application.getModel()
                : application.getEvent().getOrganizer();
    }
}
