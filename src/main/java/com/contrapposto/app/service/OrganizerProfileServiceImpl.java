package com.contrapposto.app.service;

import com.contrapposto.app.dto.OrganizerProfileRequest;
import com.contrapposto.app.model.OrganizerProfile;
import com.contrapposto.app.model.User;
import com.contrapposto.app.repository.OrganizerProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OrganizerProfileServiceImpl implements OrganizerProfileService {

    private final OrganizerProfileRepository organizerProfileRepository;

    public OrganizerProfileServiceImpl(OrganizerProfileRepository organizerProfileRepository) {
        this.organizerProfileRepository = organizerProfileRepository;
    }

    @Override
    public OrganizerProfile getOrCreateProfile(User user) {
        return organizerProfileRepository.findById(user.getId())
                .orElseGet(() -> new OrganizerProfile(user));
    }

    @Override
    public OrganizerProfile updateProfile(User user, OrganizerProfileRequest request) {
        OrganizerProfile profile = getOrCreateProfile(user);
        profile.setDisplayName(request.getDisplayName());
        profile.setOrgInfo(request.getOrgInfo());
        profile.setCity(request.getCity());
        return organizerProfileRepository.save(profile);
    }

    @Override
    public Optional<OrganizerProfile> findByUser(User user) {
        return organizerProfileRepository.findById(user.getId());
    }
}
