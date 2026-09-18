package com.contrapposto.app.controller;

import com.contrapposto.app.config.SecurityConfig;
import com.contrapposto.app.model.ModelProfile;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.User;
import com.contrapposto.app.security.CustomUserDetailsService;
import com.contrapposto.app.security.FormLoginSuccessHandler;
import com.contrapposto.app.security.OAuth2AuthenticationSuccessHandler;
import com.contrapposto.app.security.UserPrincipal;
import com.contrapposto.app.service.ModelSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ModelSearchController.class)
@Import(SecurityConfig.class)
class ModelSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModelSearchService modelSearchService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private FormLoginSuccessHandler formLoginSuccessHandler;

    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Test
    void search_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/organizer/models"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void search_wrongRole_isForbidden() throws Exception {
        mockMvc.perform(get("/organizer/models").with(user(modelPrincipal())))
                .andExpect(status().isForbidden());
    }

    @Test
    void search_asOrganizer_noCityParam_returnsAllResults() throws Exception {
        when(modelSearchService.search(isNull())).thenReturn(List.of(new ModelProfile(modelUser())));

        mockMvc.perform(get("/organizer/models").with(user(organizerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(view().name("organizer/models"))
                .andExpect(model().attributeExists("profiles"));
    }

    @Test
    void search_asOrganizer_withCityParam_passesCityThrough() throws Exception {
        when(modelSearchService.search(eq("Portland"))).thenReturn(List.of());

        mockMvc.perform(get("/organizer/models").param("city", "Portland").with(user(organizerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("city", "Portland"));
    }

    @Test
    void view_visibleProfile_returnsOk() throws Exception {
        when(modelSearchService.findVisibleProfile(7L)).thenReturn(Optional.of(new ModelProfile(modelUser())));

        mockMvc.perform(get("/organizer/models/7").with(user(organizerPrincipal())))
                .andExpect(status().isOk())
                .andExpect(view().name("organizer/model-detail"))
                .andExpect(model().attributeExists("profile"));
    }

    @Test
    void view_notVisibleOrMissing_returnsNotFound() throws Exception {
        when(modelSearchService.findVisibleProfile(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/organizer/models/999").with(user(organizerPrincipal())))
                .andExpect(status().isNotFound());
    }

    // --- Helpers ---

    private User modelUser() {
        return User.builder().id(2L).email("model@example.com").role(Role.MODEL).enabled(true).build();
    }

    private UserPrincipal organizerPrincipal() {
        return new UserPrincipal(User.builder().id(1L).email("organizer@example.com").role(Role.ORGANIZER).enabled(true).build());
    }

    private UserPrincipal modelPrincipal() {
        return new UserPrincipal(modelUser());
    }
}
