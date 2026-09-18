package com.contrapposto.app.service;

import com.contrapposto.app.dto.ModelProfileRequest;
import com.contrapposto.app.model.ModelProfile;
import com.contrapposto.app.model.User;
import com.contrapposto.app.repository.ModelProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ModelProfileServiceImpl implements ModelProfileService {

    static final int MAX_PHOTOS = 3;

    private final ModelProfileRepository modelProfileRepository;
    private final PhotoStorageService photoStorageService;

    public ModelProfileServiceImpl(ModelProfileRepository modelProfileRepository,
                                   PhotoStorageService photoStorageService) {
        this.modelProfileRepository = modelProfileRepository;
        this.photoStorageService = photoStorageService;
    }

    @Override
    public ModelProfile getOrCreateProfile(User user) {
        return modelProfileRepository.findById(user.getId())
                .orElseGet(() -> new ModelProfile(user));
    }

    @Override
    public ModelProfile updateProfile(User user, ModelProfileRequest request) {
        ModelProfile profile = getOrCreateProfile(user);
        profile.setDisplayName(request.getDisplayName());
        profile.setBio(request.getBio());
        profile.setContactInfo(request.getContactInfo());
        profile.setSocialMediaLinks(request.getSocialMediaLinks());
        profile.setCity(request.getCity());
        return modelProfileRepository.save(profile);
    }

    @Override
    public ModelProfile addPhoto(User user, MultipartFile file) {
        ModelProfile profile = getOrCreateProfile(user);
        if (profile.getPhotoUrls().size() >= MAX_PHOTOS) {
            throw new IllegalArgumentException("You can only have up to " + MAX_PHOTOS + " photos");
        }
        String extension = PhotoValidator.validate(file);
        String url = photoStorageService.upload(file, "model-" + user.getId(), extension);
        profile.getPhotoUrls().add(url);
        return modelProfileRepository.save(profile);
    }

    @Override
    public ModelProfile removePhoto(User user, String photoUrl) {
        ModelProfile profile = getOrCreateProfile(user);
        if (profile.getPhotoUrls().remove(photoUrl)) {
            photoStorageService.delete(photoUrl);
        }
        return modelProfileRepository.save(profile);
    }
}
