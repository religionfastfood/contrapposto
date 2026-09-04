package com.contrapposto.app.security;

import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.User;
import com.contrapposto.app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {

    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

    @Test
    void loadUserByUsername_existingEmail_returnsUserPrincipalWrappingUser() {
        User user = User.builder()
                .email("test@example.com")
                .role(Role.MODEL)
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        UserPrincipal principal = (UserPrincipal) service.loadUserByUsername("test@example.com");

        assertThat(principal.getUser()).isEqualTo(user);
        assertThat(principal.getUsername()).isEqualTo("test@example.com");
    }

    @Test
    void loadUserByUsername_unknownEmail_throwsUsernameNotFoundException() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("missing@example.com");
    }
}
