package com.contrapposto.app.repository;

import com.contrapposto.app.model.Event;
import com.contrapposto.app.model.EventType;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventTypeRepository eventTypeRepository;

    @Autowired
    private UserRepository userRepository;

    private User saveOrganizer(String email) {
        return userRepository.save(User.builder().email(email).password("hashed").role(Role.ORGANIZER).build());
    }

    private Event newEvent(User organizer, EventType type, String city, LocalDateTime startTime) {
        Event event = new Event(organizer);
        event.setEventType(type);
        event.setTitle("Test Event");
        event.setCity(city);
        event.setLocation("123 Main St");
        event.setStartTime(startTime);
        event.setPriceAmount(new BigDecimal("15.00"));
        event.setPriceCurrency("USD");
        return event;
    }

    @Test
    void save_thenFindById_persistsAllFields() {
        User organizer = saveOrganizer("organizer1@example.com");
        EventType type = eventTypeRepository.save(new EventType("Gesture"));
        Event event = newEvent(organizer, type, "Portland", LocalDateTime.now().plusDays(1));
        event.setDescription("Bring your own supplies");
        event.setExternalLink("https://example.com/rsvp");

        Event saved = eventRepository.save(event);
        Event found = eventRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getOrganizer().getId()).isEqualTo(organizer.getId());
        assertThat(found.getEventType().getName()).isEqualTo("Gesture");
        assertThat(found.getTitle()).isEqualTo("Test Event");
        assertThat(found.getDescription()).isEqualTo("Bring your own supplies");
        assertThat(found.getCity()).isEqualTo("Portland");
        assertThat(found.getLocation()).isEqualTo("123 Main St");
        assertThat(found.getPriceAmount()).isEqualByComparingTo("15.00");
        assertThat(found.getPriceCurrency()).isEqualTo("USD");
        assertThat(found.getExternalLink()).isEqualTo("https://example.com/rsvp");
    }

    @Test
    void findByOrganizerOrderByStartTimeDesc_onlyReturnsThatOrganizersEvents_newestFirst() {
        User organizerA = saveOrganizer("organizerA@example.com");
        User organizerB = saveOrganizer("organizerB@example.com");
        EventType type = eventTypeRepository.save(new EventType("Gesture"));
        LocalDateTime now = LocalDateTime.now();
        eventRepository.save(newEvent(organizerA, type, "Portland", now.plusDays(1)));
        eventRepository.save(newEvent(organizerA, type, "Portland", now.plusDays(5)));
        eventRepository.save(newEvent(organizerB, type, "Austin", now.plusDays(2)));

        List<Event> found = eventRepository.findByOrganizerOrderByStartTimeDesc(organizerA);

        assertThat(found).hasSize(2);
        assertThat(found.get(0).getStartTime()).isAfter(found.get(1).getStartTime());
        assertThat(found).allSatisfy(e -> assertThat(e.getOrganizer().getId()).isEqualTo(organizerA.getId()));
    }

    @Test
    void findByCityIgnoreCaseAndStartTimeAfter_excludesPastEventsAndOtherCities() {
        User organizer = saveOrganizer("organizer2@example.com");
        EventType type = eventTypeRepository.save(new EventType("Gesture"));
        LocalDateTime now = LocalDateTime.now();
        Event upcomingPortland = eventRepository.save(newEvent(organizer, type, "Portland", now.plusDays(3)));
        eventRepository.save(newEvent(organizer, type, "Portland", now.minusDays(3))); // past
        eventRepository.save(newEvent(organizer, type, "Austin", now.plusDays(3))); // other city

        List<Event> found = eventRepository.findByCityIgnoreCaseAndStartTimeAfterOrderByStartTimeAsc("portland", now);

        assertThat(found).extracting(Event::getId).containsExactly(upcomingPortland.getId());
    }

    @Test
    void existsByEventType_reflectsWhetherAnyEventUsesIt() {
        User organizer = saveOrganizer("organizer3@example.com");
        EventType used = eventTypeRepository.save(new EventType("Gesture"));
        EventType unused = eventTypeRepository.save(new EventType("Portrait"));
        eventRepository.save(newEvent(organizer, used, "Portland", LocalDateTime.now().plusDays(1)));

        assertThat(eventRepository.existsByEventType(used)).isTrue();
        assertThat(eventRepository.existsByEventType(unused)).isFalse();
    }
}
