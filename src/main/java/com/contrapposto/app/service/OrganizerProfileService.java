package com.contrapposto.app.service;

import com.contrapposto.app.dto.OrganizerProfileRequest;
import com.contrapposto.app.model.OrganizerProfile;
import com.contrapposto.app.model.User;

import java.util.Optional;

public interface OrganizerProfileService {

    OrganizerProfile getOrCreateProfile(User user);

    OrganizerProfile updateProfile(User user, OrganizerProfileRequest request);

    /**
     * Read-only lookup that does not create a profile if one doesn't exist yet -- for displaying
     * an organizer's info on a public page without side effects.
     */
    Optional<OrganizerProfile> findByUser(User user);
}
