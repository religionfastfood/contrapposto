package com.contrapposto.app.config;

import com.contrapposto.app.model.EventType;
import com.contrapposto.app.repository.EventTypeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the default event types every startup (idempotent). Unlike DevUserSeeder, this always
 * runs -- EventType is real reference data the app needs to function, not dev-only test fixtures.
 * Admins can add more via /admin/event-types; organizers can only pick from the existing list.
 */
@Component
public class EventTypeSeeder implements CommandLineRunner {

    private static final List<String> DEFAULT_TYPES =
            List.of("Gesture", "Short Pose", "Long Pose", "Portrait", "Open Studio");

    private final EventTypeRepository eventTypeRepository;

    public EventTypeSeeder(EventTypeRepository eventTypeRepository) {
        this.eventTypeRepository = eventTypeRepository;
    }

    @Override
    public void run(String... args) {
        for (String name : DEFAULT_TYPES) {
            if (!eventTypeRepository.existsByNameIgnoreCase(name)) {
                eventTypeRepository.save(new EventType(name));
            }
        }
    }
}
