package com.contrapposto.app.repository;

import com.contrapposto.app.model.ApplicationInitiator;
import com.contrapposto.app.model.ApplicationStatus;
import com.contrapposto.app.model.Event;
import com.contrapposto.app.model.EventApplication;
import com.contrapposto.app.model.EventType;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EventApplicationRepositoryTest {

    @Autowired
    private EventApplicationRepository eventApplicationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventTypeRepository eventTypeRepository;

    @Autowired
    private UserRepository userRepository;

    private User saveUser(String email, Role role) {
        return userRepository.save(User.builder().email(email).password("hashed").role(role).build());
    }

    private EventType eventType;

    private Event saveEvent(User organizer) {
        if (eventType == null) {
            eventType = eventTypeRepository.save(new EventType("Gesture"));
        }
        Event event = new Event(organizer);
        event.setEventType(eventType);
        event.setTitle("Gesture Night");
        event.setCity("Portland");
        event.setLocation("123 Main St");
        event.setStartTime(LocalDateTime.now().plusDays(1));
        return eventRepository.save(event);
    }

    @Test
    void existsByEventAndModelAndStatusIn_trueOnlyForMatchingStatuses() {
        User organizer = saveUser("organizer1@example.com", Role.ORGANIZER);
        User model = saveUser("model1@example.com", Role.MODEL);
        Event event = saveEvent(organizer);
        EventApplication application = new EventApplication(event, model, ApplicationInitiator.MODEL, null);
        eventApplicationRepository.save(application);

        assertThat(eventApplicationRepository.existsByEventAndModelAndStatusIn(event, model, List.of(ApplicationStatus.PENDING))).isTrue();
        assertThat(eventApplicationRepository.existsByEventAndModelAndStatusIn(event, model, List.of(ApplicationStatus.DECLINED))).isFalse();
    }

    @Test
    void existsByEventAndModelAndStatus_reflectsPriorDecline() {
        User organizer = saveUser("organizer2@example.com", Role.ORGANIZER);
        User model = saveUser("model2@example.com", Role.MODEL);
        Event event = saveEvent(organizer);
        EventApplication application = new EventApplication(event, model, ApplicationInitiator.MODEL, null);
        application.setStatus(ApplicationStatus.DECLINED);
        eventApplicationRepository.save(application);

        assertThat(eventApplicationRepository.existsByEventAndModelAndStatus(event, model, ApplicationStatus.DECLINED)).isTrue();
        assertThat(eventApplicationRepository.existsByEventAndModelAndStatus(event, model, ApplicationStatus.ACCEPTED)).isFalse();
    }

    @Test
    void findByModelOrderByCreatedAtDesc_onlyReturnsThatModelsRows() {
        User organizer = saveUser("organizer3@example.com", Role.ORGANIZER);
        User modelA = saveUser("modelA@example.com", Role.MODEL);
        User modelB = saveUser("modelB@example.com", Role.MODEL);
        Event event = saveEvent(organizer);
        eventApplicationRepository.save(new EventApplication(event, modelA, ApplicationInitiator.MODEL, null));
        eventApplicationRepository.save(new EventApplication(event, modelB, ApplicationInitiator.ORGANIZER, null));

        List<EventApplication> found = eventApplicationRepository.findByModelOrderByCreatedAtDesc(modelA);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getModel().getId()).isEqualTo(modelA.getId());
    }

    @Test
    void findByEvent_OrganizerOrderByCreatedAtDesc_coversBothDirectionsAcrossOrganizersEvents() {
        User organizer = saveUser("organizer4@example.com", Role.ORGANIZER);
        User otherOrganizer = saveUser("organizer5@example.com", Role.ORGANIZER);
        User model = saveUser("model3@example.com", Role.MODEL);
        Event ownEvent = saveEvent(organizer);
        Event otherEvent = saveEvent(otherOrganizer);
        eventApplicationRepository.save(new EventApplication(ownEvent, model, ApplicationInitiator.MODEL, null));
        eventApplicationRepository.save(new EventApplication(ownEvent, model, ApplicationInitiator.ORGANIZER, null));
        eventApplicationRepository.save(new EventApplication(otherEvent, model, ApplicationInitiator.MODEL, null));

        List<EventApplication> found = eventApplicationRepository.findByEvent_OrganizerOrderByCreatedAtDesc(organizer);

        assertThat(found).hasSize(2);
        assertThat(found).allSatisfy(a -> assertThat(a.getEvent().getOrganizer().getId()).isEqualTo(organizer.getId()));
    }

    @Test
    void findByEventAndStatus_returnsTheAcceptedRowOnly() {
        User organizer = saveUser("organizer6@example.com", Role.ORGANIZER);
        User modelA = saveUser("modelC@example.com", Role.MODEL);
        User modelB = saveUser("modelD@example.com", Role.MODEL);
        Event event = saveEvent(organizer);
        EventApplication pending = new EventApplication(event, modelA, ApplicationInitiator.MODEL, null);
        eventApplicationRepository.save(pending);
        EventApplication accepted = new EventApplication(event, modelB, ApplicationInitiator.ORGANIZER, null);
        accepted.setStatus(ApplicationStatus.ACCEPTED);
        eventApplicationRepository.save(accepted);

        java.util.Optional<EventApplication> found = eventApplicationRepository.findByEventAndStatus(event, ApplicationStatus.ACCEPTED);

        assertThat(found).isPresent();
        assertThat(found.get().getModel().getId()).isEqualTo(modelB.getId());
    }
}
