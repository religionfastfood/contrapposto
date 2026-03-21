package com.contrapposto.app.controller;

import com.contrapposto.app.config.SecurityConfig;
import com.contrapposto.app.security.CustomUserDetailsService;
import com.contrapposto.app.security.FormLoginSuccessHandler;
import com.contrapposto.app.security.OAuth2AuthenticationSuccessHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private FormLoginSuccessHandler formLoginSuccessHandler;

    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    // --- Organizer dashboard ---

    @Test
    void organizerDashboard_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/organizer/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void organizerDashboard_asOrganizer_returnsOk() throws Exception {
        mockMvc.perform(get("/organizer/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("organizer/dashboard"));
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
    @WithMockUser(roles = "MODEL")
    void modelDashboard_asModel_returnsOk() throws Exception {
        mockMvc.perform(get("/model/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("model/dashboard"));
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void modelDashboard_asOrganizer_returnsForbidden() throws Exception {
        mockMvc.perform(get("/model/dashboard"))
                .andExpect(status().isForbidden());
    }
}
