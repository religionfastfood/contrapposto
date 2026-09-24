package com.contrapposto.app.controller;

import com.contrapposto.app.config.SecurityConfig;
import com.contrapposto.app.model.ModelProfile;
import com.contrapposto.app.model.OrganizerProfile;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.User;
import com.contrapposto.app.security.CustomUserDetailsService;
import com.contrapposto.app.security.FormLoginSuccessHandler;
import com.contrapposto.app.security.OAuth2AuthenticationSuccessHandler;
import com.contrapposto.app.security.UserPrincipal;
import com.contrapposto.app.service.ModelProfileService;
import com.contrapposto.app.service.OrganizerProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrganizerProfileService organizerProfileService;

    @MockitoBean
    private ModelProfileService modelProfileService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private FormLoginSuccessHandler formLoginSuccessHandler;

    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    private User organizerUser() {
        return User.builder().id(1L).email("organizer@example.com").role(Role.ORGANIZER).enabled(true).build();
    }

    private User modelUser() {
        return User.builder().id(2L).email("model@example.com").role(Role.MODEL).enabled(true).build();
    }

    // --- Organizer dashboard ---

    @Test
    void organizerDashboard_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/organizer/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void organizerDashboard_noProfile_returnsOkWithGenericGreeting() throws Exception {
        User organizer = organizerUser();
        when(organizerProfileService.findByUser(organizer)).thenReturn(Optional.empty());

        mockMvc.perform(get("/organizer/dashboard").with(user(new UserPrincipal(organizer))))
                .andExpect(status().isOk())
                .andExpect(view().name("organizer/dashboard"))
                .andExpect(content().string(containsString("Welcome, Organizer!")));
    }

    @Test
    void organizerDashboard_profileWithDisplayName_greetsWithFirstNameOnly() throws Exception {
        User organizer = organizerUser();
        OrganizerProfile profile = new OrganizerProfile(organizer);
        profile.setDisplayName("Jamie Rivera");
        when(organizerProfileService.findByUser(organizer)).thenReturn(Optional.of(profile));

        mockMvc.perform(get("/organizer/dashboard").with(user(new UserPrincipal(organizer))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Welcome, Jamie!")));
    }

    @Test
    void organizerDashboard_profileWithBlankDisplayName_fallsBackToGenericGreeting() throws Exception {
        User organizer = organizerUser();
        OrganizerProfile profile = new OrganizerProfile(organizer);
        profile.setDisplayName("");
        when(organizerProfileService.findByUser(organizer)).thenReturn(Optional.of(profile));

        mockMvc.perform(get("/organizer/dashboard").with(user(new UserPrincipal(organizer))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Welcome, Organizer!")));
    }

    @Test
    @WithMockUser(roles = "MODEL")
    void organizerDashboard_asModel_returnsForbidden() throws Exception {
        mockMvc.perform(get("/organizer/dashboard"))
                .andExpect(status().isForbidden());
    }

    // --- Model dashboard ---

    @Test
    void modelDashboard_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/model/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void modelDashboard_noProfile_returnsOkWithGenericGreeting() throws Exception {
        User model = modelUser();
        when(modelProfileService.findByUser(model)).thenReturn(Optional.empty());

        mockMvc.perform(get("/model/dashboard").with(user(new UserPrincipal(model))))
                .andExpect(status().isOk())
                .andExpect(view().name("model/dashboard"))
                .andExpect(content().string(containsString("Welcome, Model!")));
    }

    @Test
    void modelDashboard_profileWithDisplayName_greetsWithFirstNameOnly() throws Exception {
        User model = modelUser();
        ModelProfile profile = new ModelProfile(model);
        profile.setDisplayName("Ava Chen");
        when(modelProfileService.findByUser(model)).thenReturn(Optional.of(profile));

        mockMvc.perform(get("/model/dashboard").with(user(new UserPrincipal(model))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Welcome, Ava!")));
    }

    @Test
    void modelDashboard_profileWithBlankDisplayName_fallsBackToGenericGreeting() throws Exception {
        User model = modelUser();
        ModelProfile profile = new ModelProfile(model);
        profile.setDisplayName("");
        when(modelProfileService.findByUser(model)).thenReturn(Optional.of(profile));

        mockMvc.perform(get("/model/dashboard").with(user(new UserPrincipal(model))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Welcome, Model!")));
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void modelDashboard_asOrganizer_returnsForbidden() throws Exception {
        mockMvc.perform(get("/model/dashboard"))
                .andExpect(status().isForbidden());
    }

    // --- Admin dashboard ---

    @Test
    void adminDashboard_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminDashboard_asAdmin_returnsOk() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void adminDashboard_asOrganizer_returnsForbidden() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MODEL")
    void adminDashboard_asModel_returnsForbidden() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }
}
