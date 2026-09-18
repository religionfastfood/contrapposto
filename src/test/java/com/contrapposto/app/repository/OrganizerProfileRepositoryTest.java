package com.contrapposto.app.repository;

import com.contrapposto.app.model.OrganizerProfile;
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
class OrganizerProfileRepositoryTest {

    @Autowired
    private OrganizerProfileRepository organizerProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void save_thenFindById_persistsAllFields() {
        User user = userRepository.save(User.builder()
                .email("organizer@example.com")
                .password("hashed")
                .role(Role.ORGANIZER)
                .build());

        OrganizerProfile profile = new OrganizerProfile(user);
        profile.setDisplayName("Life Drawing Co");
        profile.setOrgInfo("We run weekly sessions");
        profile.setCity("Austin");
        organizerProfileRepository.save(profile);

        Optional<OrganizerProfile> found = organizerProfileRepository.findById(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getDisplayName()).isEqualTo("Life Drawing Co");
        assertThat(found.get().getOrgInfo()).isEqualTo("We run weekly sessions");
        assertThat(found.get().getCity()).isEqualTo("Austin");
    }

    @Test
    void save_sharesPrimaryKeyWithUser() {
        User user = userRepository.save(User.builder()
                .email("organizer2@example.com")
                .password("hashed")
                .role(Role.ORGANIZER)
                .build());

        OrganizerProfile saved = organizerProfileRepository.save(new OrganizerProfile(user));

        assertThat(saved.getId()).isEqualTo(user.getId());
    }

    @Test
    void findById_noProfileYet_returnsEmpty() {
        assertThat(organizerProfileRepository.findById(999L)).isEmpty();
    }
}
