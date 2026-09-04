package com.contrapposto.app.security;

import com.contrapposto.app.model.AuthProvider;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.SubscriptionStatus;
import com.contrapposto.app.model.User;
import com.contrapposto.app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuth2AuthenticationSuccessHandlerTest {

    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final OAuth2AuthenticationSuccessHandler handler = new OAuth2AuthenticationSuccessHandler(userRepository);
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    // --- Existing user ---

    @Test
    void onAuthenticationSuccess_existingAdmin_redirectsToAdminDashboard() throws Exception {
        User user = User.builder().email("a@example.com").role(Role.ADMIN)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build();
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(user));

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, tokenFor("a@example.com", null));

        assertThat(response.getRedirectedUrl()).isEqualTo("/admin/dashboard");
        verify(userRepository, never()).save(any());
    }

    @Test
    void onAuthenticationSuccess_existingUserWithNoneStatus_redirectsToPlanPage() throws Exception {
        User user = User.builder().email("m@example.com").role(Role.MODEL)
                .subscriptionStatus(SubscriptionStatus.NONE).build();
        when(userRepository.findByEmail("m@example.com")).thenReturn(Optional.of(user));

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, tokenFor("m@example.com", null));

        assertThat(response.getRedirectedUrl()).isEqualTo("/subscription/plan");
    }

    @Test
    void onAuthenticationSuccess_existingOrganizerWithTrial_redirectsToOrganizerDashboard() throws Exception {
        User user = User.builder().email("o@example.com").role(Role.ORGANIZER)
                .subscriptionStatus(SubscriptionStatus.TRIAL).build();
        when(userRepository.findByEmail("o@example.com")).thenReturn(Optional.of(user));

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, tokenFor("o@example.com", null));

        assertThat(response.getRedirectedUrl()).isEqualTo("/organizer/dashboard");
    }

    @Test
    void onAuthenticationSuccess_existingModelWithActive_redirectsToModelDashboard() throws Exception {
        User user = User.builder().email("m2@example.com").role(Role.MODEL)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build();
        when(userRepository.findByEmail("m2@example.com")).thenReturn(Optional.of(user));

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, tokenFor("m2@example.com", null));

        assertThat(response.getRedirectedUrl()).isEqualTo("/model/dashboard");
    }

    // --- New user ---

    @Test
    void onAuthenticationSuccess_newUserNoSession_redirectsToRegisterPending() throws Exception {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, tokenFor("new@example.com", null));

        assertThat(response.getRedirectedUrl()).isEqualTo("/register?oauth=pending");
        verify(userRepository, never()).save(any());
    }

    @Test
    void onAuthenticationSuccess_newUserNoPendingRoleInSession_redirectsToRegisterPending() throws Exception {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true);

        handler.onAuthenticationSuccess(request, response, tokenFor("new@example.com", null));

        assertThat(response.getRedirectedUrl()).isEqualTo("/register?oauth=pending");
        verify(userRepository, never()).save(any());
    }

    @Test
    void onAuthenticationSuccess_newUserPendingAdminRole_redirectsToRegisterPending() throws Exception {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute("pendingRole", Role.ADMIN);

        handler.onAuthenticationSuccess(request, response, tokenFor("new@example.com", null));

        assertThat(response.getRedirectedUrl()).isEqualTo("/register?oauth=pending");
        verify(userRepository, never()).save(any());
    }

    @Test
    void onAuthenticationSuccess_newUserPendingModelRole_createsUserAndRedirectsToPlanPage() throws Exception {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute("pendingRole", Role.MODEL);

        handler.onAuthenticationSuccess(request, response, tokenFor("new@example.com", "google-sub-123"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new@example.com");
        assertThat(saved.getRole()).isEqualTo(Role.MODEL);
        assertThat(saved.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(saved.getProviderId()).isEqualTo("google-sub-123");
        assertThat(saved.isEnabled()).isTrue();
        assertThat(response.getRedirectedUrl()).isEqualTo("/subscription/plan");
        assertThat(request.getSession(false).getAttribute("pendingRole")).isNull();
    }

    @Test
    void onAuthenticationSuccess_newUserPendingOrganizerRole_createsOrganizerAndRedirectsToPlanPage() throws Exception {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute("pendingRole", Role.ORGANIZER);

        handler.onAuthenticationSuccess(request, response, tokenFor("new@example.com", "google-sub-456"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.ORGANIZER);
        assertThat(response.getRedirectedUrl()).isEqualTo("/subscription/plan");
    }

    private OAuth2AuthenticationToken tokenFor(String email, String providerId) {
        OAuth2User oAuth2User = Mockito.mock(OAuth2User.class);
        when(oAuth2User.getAttribute("email")).thenReturn(email);
        when(oAuth2User.getName()).thenReturn(providerId);
        OAuth2AuthenticationToken token = Mockito.mock(OAuth2AuthenticationToken.class);
        when(token.getPrincipal()).thenReturn(oAuth2User);
        return token;
    }
}
