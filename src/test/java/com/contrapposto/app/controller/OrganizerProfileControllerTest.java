package com.contrapposto.app.controller;

import com.contrapposto.app.config.SecurityConfig;
import com.contrapposto.app.model.OrganizerProfile;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.User;
import com.contrapposto.app.security.CustomUserDetailsService;
import com.contrapposto.app.security.FormLoginSuccessHandler;
import com.contrapposto.app.security.OAuth2AuthenticationSuccessHandler;
import com.contrapposto.app.security.UserPrincipal;
import com.contrapposto.app.service.OrganizerProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrganizerProfileController.class)
@Import(SecurityConfig.class)
class OrganizerProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrganizerProfileService organizerProfileService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private FormLoginSuccessHandler formLoginSuccessHandler;

    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Test
    void getProfile_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/organizer/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void getProfile_wrongRole_isForbidden() throws Exception {
        mockMvc.perform(get("/organizer/profile").with(user(modelPrincipal())))
                .andExpect(status().isForbidden());
    }

    @Test
    void getProfile_asOrganizer_returnsOk() throws Exception {
        User user = organizerUser();
        when(organizerProfileService.getOrCreateProfile(any())).thenReturn(new OrganizerProfile(user));

        mockMvc.perform(get("/organizer/profile").with(user(new UserPrincipal(user))))
                .andExpect(status().isOk())
                .andExpect(view().name("organizer/profile"))
                .andExpect(model().attributeExists("profile", "profileRequest"));
    }

    @Test
    void postProfile_validData_updatesAndReturnsOk() throws Exception {
        User user = organizerUser();
        when(organizerProfileService.updateProfile(any(), any())).thenReturn(new OrganizerProfile(user));
        when(organizerProfileService.getOrCreateProfile(any())).thenReturn(new OrganizerProfile(user));

        mockMvc.perform(post("/organizer/profile").with(csrf()).with(user(new UserPrincipal(user)))
                        .param("displayName", "Life Drawing Co")
                        .param("city", "Austin"))
                .andExpect(status().isOk())
                .andExpect(view().name("organizer/profile"))
                .andExpect(model().attributeExists("success"));
    }

    @Test
    void postProfile_orgInfoTooLong_returnsFormWithFieldError() throws Exception {
        User user = organizerUser();
        when(organizerProfileService.getOrCreateProfile(any())).thenReturn(new OrganizerProfile(user));

        mockMvc.perform(post("/organizer/profile").with(csrf()).with(user(new UserPrincipal(user)))
                        .param("orgInfo", "x".repeat(2001)))
                .andExpect(status().isOk())
                .andExpect(view().name("organizer/profile"))
                .andExpect(model().attributeHasFieldErrors("profileRequest", "orgInfo"));
    }

    // --- Helpers ---

    private User organizerUser() {
        return User.builder().id(1L).email("organizer@example.com").role(Role.ORGANIZER).enabled(true).build();
    }

    private UserPrincipal modelPrincipal() {
        User user = User.builder().id(2L).email("model@example.com").role(Role.MODEL).enabled(true).build();
        return new UserPrincipal(user);
    }
}
