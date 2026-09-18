package com.contrapposto.app.repository;

import com.contrapposto.app.model.Event;
import com.contrapposto.app.model.EventType;
import com.contrapposto.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByOrganizerOrderByStartTimeDesc(User organizer);

    List<Event> findByCityIgnoreCaseAndStartTimeAfterOrderByStartTimeAsc(String city, LocalDateTime after);

    boolean existsByEventType(EventType eventType);
}
