package com.contrapposto.app.service;

import com.contrapposto.app.dto.EventRequest;
import com.contrapposto.app.model.Event;
import com.contrapposto.app.model.EventType;
import com.contrapposto.app.model.User;
import com.contrapposto.app.repository.EventRepository;
import com.contrapposto.app.repository.EventTypeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Predicate;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventTypeRepository eventTypeRepository;
    private final SubscriptionService subscriptionService;

    public EventServiceImpl(EventRepository eventRepository, EventTypeRepository eventTypeRepository,
                             SubscriptionService subscriptionService) {
        this.eventRepository = eventRepository;
        this.eventTypeRepository = eventTypeRepository;
        this.subscriptionService = subscriptionService;
    }

    @Override
    public List<Event> findByOrganizer(User organizer) {
        return eventRepository.findByOrganizerOrderByStartTimeDesc(organizer);
    }

    @Override
    public Optional<Event> findById(Long id) {
        return eventRepository.findById(id);
    }

    @Override
    public Optional<Event> findOwnedById(User organizer, Long eventId) {
        return eventRepository.findById(eventId).filter(owns(organizer));
    }

    @Override
    public List<Event> upcomingByCity(String city) {
        return eventRepository.findByCityIgnoreCaseAndStartTimeAfterOrderByStartTimeAsc(city, LocalDateTime.now());
    }

    @Override
    public Event create(User organizer, EventRequest request) {
        if (!subscriptionService.isSubscriptionActive(organizer)) {
            throw new IllegalStateException("An active subscription is required to post new events");
        }
        Event event = new Event(organizer);
        applyRequest(event, request);
        return eventRepository.save(event);
    }

    @Override
    public Optional<Event> update(User organizer, Long eventId, EventRequest request) {
        return findOwnedById(organizer, eventId).map(event -> {
            applyRequest(event, request);
            return eventRepository.save(event);
        });
    }

    @Override
    public boolean delete(User organizer, Long eventId) {
        Optional<Event> owned = findOwnedById(organizer, eventId);
        owned.ifPresent(eventRepository::delete);
        return owned.isPresent();
    }

    private static Predicate<Event> owns(User organizer) {
        return event -> event.getOrganizer().getId().equals(organizer.getId());
    }

    private void applyRequest(Event event, EventRequest request) {
        EventType eventType = eventTypeRepository.findById(request.getEventTypeId())
                .orElseThrow(() -> new NoSuchElementException("Event type not found"));
        event.setEventType(eventType);
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setCity(request.getCity());
        event.setLocation(request.getLocation());
        event.setStartTime(request.getStartTime());
        event.setPriceAmount(request.getPriceAmount());
        event.setPriceCurrency(request.getPriceAmount() != null ? "USD" : null);
        event.setExternalLink(request.getExternalLink());
    }
}
