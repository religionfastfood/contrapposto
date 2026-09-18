package com.contrapposto.app.service;

import com.contrapposto.app.model.EventType;
import com.contrapposto.app.repository.EventRepository;
import com.contrapposto.app.repository.EventTypeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class EventTypeServiceImpl implements EventTypeService {

    private final EventTypeRepository eventTypeRepository;
    private final EventRepository eventRepository;

    public EventTypeServiceImpl(EventTypeRepository eventTypeRepository, EventRepository eventRepository) {
        this.eventTypeRepository = eventTypeRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    public List<EventType> findAll() {
        return eventTypeRepository.findAllByOrderByNameAsc();
    }

    @Override
    public EventType create(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (eventTypeRepository.existsByNameIgnoreCase(trimmed)) {
            throw new IllegalArgumentException("An event type named \"" + trimmed + "\" already exists");
        }
        return eventTypeRepository.save(new EventType(trimmed));
    }

    @Override
    public void delete(Long id) {
        EventType eventType = eventTypeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Event type not found"));
        if (eventRepository.existsByEventType(eventType)) {
            throw new IllegalStateException("Cannot delete \"" + eventType.getName() + "\" -- it's still used by existing events");
        }
        eventTypeRepository.delete(eventType);
    }
}
