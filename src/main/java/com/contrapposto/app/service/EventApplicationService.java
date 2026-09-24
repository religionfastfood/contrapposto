package com.contrapposto.app.service;

import com.contrapposto.app.model.Event;
import com.contrapposto.app.model.EventApplication;
import com.contrapposto.app.model.User;

import java.util.List;
import java.util.Optional;

public interface EventApplicationService {

    /**
     * A model applies to an event.
     *
     * @throws IllegalStateException if the model's subscription isn't active, or an active
     *                                application/invitation already exists for this event+model
     */
    EventApplication apply(User model, Long eventId, String message);

    /**
     * An organizer invites a model to one of their own events.
     *
     * @throws IllegalStateException if the model isn't a MODEL with an active subscription, or an
     *                                active application/invitation already exists for this event+model
     */
    EventApplication invite(User organizer, Long eventId, Long modelId, String message);

    /**
     * Accepts a PENDING application/invitation. Empty if not found, not PENDING, or actor isn't
     * the correct responder (the organizer for a model-initiated row, the model for an
     * organizer-initiated one) -- deliberately not distinguished, matching EventService's
     * ownership-lookup pattern.
     */
    Optional<EventApplication> accept(User actor, Long applicationId);

    /**
     * Declines a PENDING application/invitation. Same actor/status rules as {@link #accept}.
     */
    Optional<EventApplication> decline(User actor, Long applicationId);

    /**
     * Withdraws a PENDING application/invitation. Empty if not found, not PENDING, or actor isn't
     * the original initiator (the model for a model-initiated row, the organizer for an
     * organizer-initiated one).
     */
    Optional<EventApplication> withdraw(User actor, Long applicationId);

    /**
     * A model's own applications and the invitations they've received, newest first.
     */
    List<EventApplication> findForModel(User model);

    /**
     * Applications received and invitations sent across all of an organizer's events, newest first.
     */
    List<EventApplication> findForOrganizer(User organizer);

    /**
     * The model's active (PENDING/ACCEPTED) application or invitation for this event, if any.
     */
    Optional<EventApplication> findActiveForEventAndModel(Event event, User model);

    /**
     * True if this model has a prior DECLINED row for this event -- surfaced to organizers as a
     * hint, not enforced as a block.
     */
    boolean hasPriorDecline(Event event, User model);
}
