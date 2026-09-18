package com.contrapposto.app.service;

import com.contrapposto.app.dto.EventRequest;
import com.contrapposto.app.model.Event;
import com.contrapposto.app.model.User;

import java.util.List;
import java.util.Optional;

public interface EventService {

    List<Event> findByOrganizer(User organizer);

    Optional<Event> findById(Long id);

    /**
     * Empty if eventId doesn't exist or doesn't belong to organizer.
     */
    Optional<Event> findOwnedById(User organizer, Long eventId);

    /**
     * Upcoming (startTime in the future) events in the given city, soonest first.
     */
    List<Event> upcomingByCity(String city);

    /**
     * @throws IllegalStateException if the organizer's subscription isn't active
     */
    Event create(User organizer, EventRequest request);

    /**
     * Empty if eventId doesn't exist or doesn't belong to organizer -- deliberately not
     * distinguished, so a non-owner gets the same "not found" response as a bad id.
     */
    Optional<Event> update(User organizer, Long eventId, EventRequest request);

    /**
     * @return true if deleted; false if eventId doesn't exist or doesn't belong to organizer
     */
    boolean delete(User organizer, Long eventId);
}
