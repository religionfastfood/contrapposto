package com.contrapposto.app.repository;

import com.contrapposto.app.model.ModelProfile;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the real schema against the test DB — unlike the mocked-repository
 * unit tests, this actually catches DDL issues (e.g. an invalid columnDefinition
 * for the HSQLDB dialect silently failing table creation at startup).
 */
@DataJpaTest
class ModelProfileRepositoryTest {

    @Autowired
    private ModelProfileRepository modelProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void save_thenFindById_persistsAllFieldsAndPhotos() {
        User user = userRepository.save(User.builder()
                .email("model@example.com")
                .password("hashed")
                .role(Role.MODEL)
                .build());

        ModelProfile profile = new ModelProfile(user);
        profile.setBio("A bio");
        profile.setContactInfo("555-1234");
        profile.setSocialMediaLinks("instagram.com/me");
        profile.setCity("Portland");
        profile.getPhotoUrls().add("/uploads/1.jpg");
        profile.getPhotoUrls().add("/uploads/2.jpg");
        modelProfileRepository.save(profile);

        Optional<ModelProfile> found = modelProfileRepository.findById(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getBio()).isEqualTo("A bio");
        assertThat(found.get().getContactInfo()).isEqualTo("555-1234");
        assertThat(found.get().getSocialMediaLinks()).isEqualTo("instagram.com/me");
        assertThat(found.get().getCity()).isEqualTo("Portland");
        assertThat(found.get().getPhotoUrls()).containsExactly("/uploads/1.jpg", "/uploads/2.jpg");
    }

    @Test
    void save_sharesPrimaryKeyWithUser() {
        User user = userRepository.save(User.builder()
                .email("model2@example.com")
                .password("hashed")
                .role(Role.MODEL)
                .build());

        ModelProfile saved = modelProfileRepository.save(new ModelProfile(user));

        assertThat(saved.getId()).isEqualTo(user.getId());
    }

    @Test
    void findById_noProfileYet_returnsEmpty() {
        assertThat(modelProfileRepository.findById(999L)).isEmpty();
    }
}
