package com.contrapposto.app.service;

import com.contrapposto.app.event.ApplicationDecidedEvent;
import com.contrapposto.app.event.ApplicationSubmittedEvent;
import com.contrapposto.app.model.ApplicationInitiator;
import com.contrapposto.app.model.Event;
import com.contrapposto.app.model.EventApplication;
import com.contrapposto.app.model.EventType;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.User;
import com.contrapposto.app.repository.EventApplicationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Calls the @Async @TransactionalEventListener methods directly -- outside a Spring context,
// those annotations are inert, so this exercises the plain method body synchronously.
class ApplicationNotificationListenerTest {

    private final EventApplicationRepository eventApplicationRepository = Mockito.mock(EventApplicationRepository.class);
    private final NotificationService notificationService = Mockito.mock(NotificationService.class);
    private final ApplicationNotificationListener listener =
            new ApplicationNotificationListener(eventApplicationRepository, notificationService);

    private User organizer() {
        return User.builder().id(1L).email("organizer@example.com").role(Role.ORGANIZER).build();
    }

    private User model() {
        return User.builder().id(2L).email("model@example.com").role(Role.MODEL).build();
    }

    @Test
    void onSubmitted_modelInitiated_notifiesOrganizer() {
        User organizer = organizer();
        Event event = new Event(organizer);
        event.setEventType(new EventType("Gesture"));
        event.setTitle("Gesture Night");
        EventApplication application = new EventApplication(event, model(), ApplicationInitiator.MODEL, null);
        application.setId(1L);
        when(eventApplicationRepository.findById(1L)).thenReturn(Optional.of(application));

        listener.onSubmitted(new ApplicationSubmittedEvent(1L));

        verify(notificationService).notifySubmitted(application, organizer);
    }

    @Test
    void onSubmitted_organizerInitiated_notifiesModel() {
        User organizer = organizer();
        User model = model();
        Event event = new Event(organizer);
        event.setEventType(new EventType("Gesture"));
        EventApplication application = new EventApplication(event, model, ApplicationInitiator.ORGANIZER, null);
        application.setId(2L);
        when(eventApplicationRepository.findById(2L)).thenReturn(Optional.of(application));

        listener.onSubmitted(new ApplicationSubmittedEvent(2L));

        verify(notificationService).notifySubmitted(application, model);
    }

    @Test
    void onDecided_modelInitiated_notifiesModel() {
        User model = model();
        Event event = new Event(organizer());
        event.setEventType(new EventType("Gesture"));
        EventApplication application = new EventApplication(event, model, ApplicationInitiator.MODEL, null);
        application.setId(3L);
        when(eventApplicationRepository.findById(3L)).thenReturn(Optional.of(application));

        listener.onDecided(new ApplicationDecidedEvent(3L));

        verify(notificationService).notifyDecided(application, model);
    }

    @Test
    void onDecided_organizerInitiated_notifiesOrganizer() {
        User organizer = organizer();
        Event event = new Event(organizer);
        event.setEventType(new EventType("Gesture"));
        EventApplication application = new EventApplication(event, model(), ApplicationInitiator.ORGANIZER, null);
        application.setId(4L);
        when(eventApplicationRepository.findById(4L)).thenReturn(Optional.of(application));

        listener.onDecided(new ApplicationDecidedEvent(4L));

        verify(notificationService).notifyDecided(application, organizer);
    }
}
