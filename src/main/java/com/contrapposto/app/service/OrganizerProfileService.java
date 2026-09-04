package com.contrapposto.app.service;

import com.contrapposto.app.dto.OrganizerProfileRequest;
import com.contrapposto.app.model.OrganizerProfile;
import com.contrapposto.app.model.User;

public interface OrganizerProfileService {

    OrganizerProfile getOrCreateProfile(User user);

    OrganizerProfile updateProfile(User user, OrganizerProfileRequest request);
}
