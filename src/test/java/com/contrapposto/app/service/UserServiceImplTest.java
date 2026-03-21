package com.contrapposto.app.service;

import com.contrapposto.app.dto.RegisterRequest;
import com.contrapposto.app.model.AuthProvider;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.User;
import com.contrapposto.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private RegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest();
        validRequest.setEmail("test@example.com");
        validRequest.setPassword("password123");
        validRequest.setConfirmPassword("password123");
        validRequest.setRole(Role.MODEL);
    }

    @Test
    void register_withValidRequest_savesAndReturnsUser() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.register(validRequest);

        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getPassword()).isEqualTo("encoded-password");
        assertThat(result.getRole()).isEqualTo(Role.MODEL);
        assertThat(result.getProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(result.isEnabled()).isTrue();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_withMismatchedPasswords_throwsIllegalArgumentException() {
        validRequest.setConfirmPassword("different-password");

        assertThatThrownBy(() -> userService.register(validRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Passwords do not match");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_withDuplicateEmail_throwsIllegalArgumentException() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(validRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("An account with that email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_encodesPasswordBeforeSaving() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.register(validRequest);

        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(argThat(u -> "encoded-password".equals(u.getPassword())));
    }

    @Test
    void register_asOrganizer_setsOrganizerRole() {
        validRequest.setRole(Role.ORGANIZER);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.register(validRequest);

        assertThat(result.getRole()).isEqualTo(Role.ORGANIZER);
    }

    @Test
    void emailExists_whenEmailFound_returnsTrue() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThat(userService.emailExists("test@example.com")).isTrue();
    }

    @Test
    void emailExists_whenEmailNotFound_returnsFalse() {
        when(userRepository.existsByEmail("unknown@example.com")).thenReturn(false);

        assertThat(userService.emailExists("unknown@example.com")).isFalse();
    }
}
