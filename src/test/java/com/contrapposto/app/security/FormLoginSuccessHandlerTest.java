package com.contrapposto.app.security;

import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.SubscriptionStatus;
import com.contrapposto.app.model.User;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class FormLoginSuccessHandlerTest {

    private final FormLoginSuccessHandler handler = new FormLoginSuccessHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @Test
    void onAuthenticationSuccess_adminRole_redirectsToAdminDashboard() throws Exception {
        handler.onAuthenticationSuccess(request, response, authenticationFor(Role.ADMIN, SubscriptionStatus.ACTIVE));

        assertThat(response.getRedirectedUrl()).isEqualTo("/admin/dashboard");
    }

    @Test
    void onAuthenticationSuccess_adminRole_ignoresSubscriptionStatus() throws Exception {
        handler.onAuthenticationSuccess(request, response, authenticationFor(Role.ADMIN, SubscriptionStatus.NONE));

        assertThat(response.getRedirectedUrl()).isEqualTo("/admin/dashboard");
    }

    @Test
    void onAuthenticationSuccess_modelWithNoneStatus_redirectsToPlanPage() throws Exception {
        handler.onAuthenticationSuccess(request, response, authenticationFor(Role.MODEL, SubscriptionStatus.NONE));

        assertThat(response.getRedirectedUrl()).isEqualTo("/subscription/plan");
    }

    @Test
    void onAuthenticationSuccess_organizerWithNoneStatus_redirectsToPlanPage() throws Exception {
        handler.onAuthenticationSuccess(request, response, authenticationFor(Role.ORGANIZER, SubscriptionStatus.NONE));

        assertThat(response.getRedirectedUrl()).isEqualTo("/subscription/plan");
    }

    @Test
    void onAuthenticationSuccess_organizerWithActiveStatus_redirectsToOrganizerDashboard() throws Exception {
        handler.onAuthenticationSuccess(request, response, authenticationFor(Role.ORGANIZER, SubscriptionStatus.ACTIVE));

        assertThat(response.getRedirectedUrl()).isEqualTo("/organizer/dashboard");
    }

    @Test
    void onAuthenticationSuccess_organizerWithTrialStatus_redirectsToOrganizerDashboard() throws Exception {
        handler.onAuthenticationSuccess(request, response, authenticationFor(Role.ORGANIZER, SubscriptionStatus.TRIAL));

        assertThat(response.getRedirectedUrl()).isEqualTo("/organizer/dashboard");
    }

    @Test
    void onAuthenticationSuccess_modelWithActiveStatus_redirectsToModelDashboard() throws Exception {
        handler.onAuthenticationSuccess(request, response, authenticationFor(Role.MODEL, SubscriptionStatus.ACTIVE));

        assertThat(response.getRedirectedUrl()).isEqualTo("/model/dashboard");
    }

    @Test
    void onAuthenticationSuccess_modelWithLapsedStatus_redirectsToModelDashboard() throws Exception {
        handler.onAuthenticationSuccess(request, response, authenticationFor(Role.MODEL, SubscriptionStatus.LAPSED));

        assertThat(response.getRedirectedUrl()).isEqualTo("/model/dashboard");
    }

    private Authentication authenticationFor(Role role, SubscriptionStatus status) {
        User user = User.builder()
                .email("test@example.com")
                .role(role)
                .subscriptionStatus(status)
                .build();
        UserPrincipal principal = new UserPrincipal(user);
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        return authentication;
    }
}
