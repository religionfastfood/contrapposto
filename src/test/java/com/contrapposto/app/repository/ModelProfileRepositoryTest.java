package com.contrapposto.app.repository;

import com.contrapposto.app.model.ModelProfile;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.SubscriptionStatus;
import com.contrapposto.app.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
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
        profile.setDisplayName("Jamie Rivera");
        profile.setBio("A bio");
        profile.setContactInfo("555-1234");
        profile.setSocialMediaLinks("instagram.com/me");
        profile.setCity("Portland");
        profile.getPhotoUrls().add("/uploads/1.jpg");
        profile.getPhotoUrls().add("/uploads/2.jpg");
        modelProfileRepository.save(profile);

        Optional<ModelProfile> found = modelProfileRepository.findById(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getDisplayName()).isEqualTo("Jamie Rivera");
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

    @Test
    void findByUser_SubscriptionStatusIn_excludesLapsedAndNoneStatuses() {
        ModelProfile active = saveProfile("active@example.com", SubscriptionStatus.ACTIVE, "Portland");
        saveProfile("lapsed@example.com", SubscriptionStatus.LAPSED, "Portland");
        saveProfile("none@example.com", SubscriptionStatus.NONE, "Portland");

        List<ModelProfile> visible = modelProfileRepository.findByUser_SubscriptionStatusIn(SubscriptionStatus.active());

        assertThat(visible).extracting(ModelProfile::getId).containsExactly(active.getId());
    }

    @Test
    void findByUser_SubscriptionStatusInAndCityIgnoreCase_filtersByCityCaseInsensitively() {
        ModelProfile portland = saveProfile("p@example.com", SubscriptionStatus.ACTIVE, "Portland");
        saveProfile("a@example.com", SubscriptionStatus.ACTIVE, "Austin");

        List<ModelProfile> visible = modelProfileRepository.findByUser_SubscriptionStatusInAndCityIgnoreCase(
                SubscriptionStatus.active(), "PORTLAND");

        assertThat(visible).extracting(ModelProfile::getId).containsExactly(portland.getId());
    }

    private ModelProfile saveProfile(String email, SubscriptionStatus status, String city) {
        User user = userRepository.save(User.builder()
                .email(email)
                .password("hashed")
                .role(Role.MODEL)
                .subscriptionStatus(status)
                .build());
        ModelProfile profile = new ModelProfile(user);
        profile.setCity(city);
        return modelProfileRepository.save(profile);
    }
}
