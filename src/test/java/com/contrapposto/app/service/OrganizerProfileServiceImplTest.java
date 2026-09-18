package com.contrapposto.app.service;

import com.contrapposto.app.dto.OrganizerProfileRequest;
import com.contrapposto.app.model.OrganizerProfile;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.User;
import com.contrapposto.app.repository.OrganizerProfileRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrganizerProfileServiceImplTest {

    private final OrganizerProfileRepository organizerProfileRepository = Mockito.mock(OrganizerProfileRepository.class);
    private final OrganizerProfileServiceImpl service = new OrganizerProfileServiceImpl(organizerProfileRepository);

    {
        when(organizerProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private User userWithId(long id) {
        return User.builder().id(id).email("organizer@example.com").role(Role.ORGANIZER).build();
    }

    @Test
    void getOrCreateProfile_noExistingProfile_returnsNewProfileForUser() {
        User user = userWithId(1L);
        when(organizerProfileRepository.findById(1L)).thenReturn(Optional.empty());

        OrganizerProfile profile = service.getOrCreateProfile(user);

        assertThat(profile.getUser()).isEqualTo(user);
    }

    @Test
    void getOrCreateProfile_existingProfile_returnsIt() {
        User user = userWithId(1L);
        OrganizerProfile existing = new OrganizerProfile(user);
        existing.setDisplayName("Existing Org");
        when(organizerProfileRepository.findById(1L)).thenReturn(Optional.of(existing));

        OrganizerProfile profile = service.getOrCreateProfile(user);

        assertThat(profile.getDisplayName()).isEqualTo("Existing Org");
    }

    @Test
    void updateProfile_setsAllFieldsAndSaves() {
        User user = userWithId(2L);
        when(organizerProfileRepository.findById(2L)).thenReturn(Optional.empty());
        OrganizerProfileRequest request = new OrganizerProfileRequest();
        request.setDisplayName("Life Drawing Co");
        request.setOrgInfo("We run weekly sessions");
        request.setCity("Austin");

        OrganizerProfile profile = service.updateProfile(user, request);

        assertThat(profile.getDisplayName()).isEqualTo("Life Drawing Co");
        assertThat(profile.getOrgInfo()).isEqualTo("We run weekly sessions");
        assertThat(profile.getCity()).isEqualTo("Austin");
        verify(organizerProfileRepository).save(profile);
    }

    @Test
    void findByUser_existingProfile_returnsItWithoutCreating() {
        User user = userWithId(3L);
        OrganizerProfile existing = new OrganizerProfile(user);
        when(organizerProfileRepository.findById(3L)).thenReturn(Optional.of(existing));

        assertThat(service.findByUser(user)).contains(existing);
    }

    @Test
    void findByUser_noProfile_returnsEmptyWithoutCreating() {
        User user = userWithId(4L);
        when(organizerProfileRepository.findById(4L)).thenReturn(Optional.empty());

        assertThat(service.findByUser(user)).isEmpty();
        verify(organizerProfileRepository, never()).save(any());
    }
}
