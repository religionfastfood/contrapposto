package com.contrapposto.app.config;

import com.contrapposto.app.model.ModelProfile;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.SubscriptionStatus;
import com.contrapposto.app.model.User;
import com.contrapposto.app.repository.ModelProfileRepository;
import com.contrapposto.app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DevModelProfileSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelProfileRepository modelProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DevModelProfileSeeder newSeeder() {
        return new DevModelProfileSeeder(userRepository, modelProfileRepository, passwordEncoder);
    }

    @Test
    void seedsAllDummyProfilesWhenNoneExist() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        newSeeder().run();

        ArgumentCaptor<ModelProfile> captor = ArgumentCaptor.forClass(ModelProfile.class);
        verify(modelProfileRepository, times(6)).save(captor.capture());
        List<ModelProfile> saved = captor.getAllValues();

        assertThat(saved).extracting(ModelProfile::getDisplayName)
                .contains("Ava Chen", "Marcus Bell", "Priya Nair", "Diego Alvarez", "Sasha Morgan", "Robin Lapsed");
        assertThat(saved).allSatisfy(profile -> {
            assertThat(profile.getBio()).isNotBlank();
            assertThat(profile.getCity()).isNotBlank();
            assertThat(profile.getPhotoUrls()).isNotEmpty();
            assertThat(profile.getUser().getRole()).isEqualTo(Role.MODEL);
        });
    }

    @Test
    void seedsExactlyOneLapsedProfile_forTestingSearchVisibility() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        newSeeder().run();

        ArgumentCaptor<ModelProfile> captor = ArgumentCaptor.forClass(ModelProfile.class);
        verify(modelProfileRepository, times(6)).save(captor.capture());

        List<ModelProfile> lapsed = captor.getAllValues().stream()
                .filter(p -> p.getUser().getSubscriptionStatus() == SubscriptionStatus.LAPSED)
                .toList();
        assertThat(lapsed).hasSize(1);
        assertThat(lapsed.get(0).getDisplayName()).isEqualTo("Robin Lapsed");
    }

    @Test
    void skipsProfilesThatAlreadyExist() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        when(userRepository.existsByEmail("model.marcus@contrapposto.local")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        newSeeder().run();

        ArgumentCaptor<ModelProfile> captor = ArgumentCaptor.forClass(ModelProfile.class);
        verify(modelProfileRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getDisplayName()).isEqualTo("Marcus Bell");
    }
}
