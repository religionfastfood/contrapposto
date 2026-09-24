package com.contrapposto.app.repository;

import com.contrapposto.app.model.ApplicationStatus;
import com.contrapposto.app.model.Event;
import com.contrapposto.app.model.EventApplication;
import com.contrapposto.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventApplicationRepository extends JpaRepository<EventApplication, Long> {

    boolean existsByEventAndModelAndStatusIn(Event event, User model, List<ApplicationStatus> statuses);

    boolean existsByEventAndModelAndStatus(Event event, User model, ApplicationStatus status);

    Optional<EventApplication> findByEventAndModelAndStatusIn(Event event, User model, List<ApplicationStatus> statuses);

    List<EventApplication> findByModelOrderByCreatedAtDesc(User model);

    List<EventApplication> findByEvent_OrganizerOrderByCreatedAtDesc(User organizer);
}
