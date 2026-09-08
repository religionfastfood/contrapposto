package com.contrapposto.app.config;

import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.SubscriptionStatus;
import com.contrapposto.app.model.User;
import com.contrapposto.app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DevUserSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DevUserSeeder newSeeder() {
        return new DevUserSeeder(userRepository, passwordEncoder,
                "admin@contrapposto.local", "admin123",
                "model@contrapposto.local", "model123",
                "organizer@contrapposto.local", "organizer123");
    }

    @Test
    void seedsAllThreeAccountsWhenNoneExist() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        newSeeder().run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(3)).save(captor.capture());
        List<User> saved = captor.getAllValues();

        assertThat(saved).extracting(User::getEmail)
                .containsExactly("admin@contrapposto.local", "model@contrapposto.local", "organizer@contrapposto.local");
        assertThat(saved).extracting(User::getRole)
                .containsExactly(Role.ADMIN, Role.MODEL, Role.ORGANIZER);

        User admin = saved.get(0);
        User model = saved.get(1);
        User organizer = saved.get(2);
        assertThat(admin.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.NONE);
        assertThat(model.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(organizer.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void skipsAccountsThatAlreadyExist() {
        when(userRepository.existsByEmail("admin@contrapposto.local")).thenReturn(true);
        when(userRepository.existsByEmail("model@contrapposto.local")).thenReturn(false);
        when(userRepository.existsByEmail("organizer@contrapposto.local")).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        newSeeder().run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("model@contrapposto.local");
    }
}
