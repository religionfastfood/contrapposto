package com.contrapposto.app.service;

import com.contrapposto.app.dto.ModelProfileRequest;
import com.contrapposto.app.model.ModelProfile;
import com.contrapposto.app.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface ModelProfileService {

    ModelProfile getOrCreateProfile(User user);

    ModelProfile updateProfile(User user, ModelProfileRequest request);

    ModelProfile addPhoto(User user, MultipartFile file);

    ModelProfile removePhoto(User user, String photoUrl);

    /**
     * Read-only lookup that does not create a profile as a side effect (unlike
     * {@link #getOrCreateProfile}) -- for looking up another user's profile for display purposes.
     */
    Optional<ModelProfile> findByUser(User user);
}
