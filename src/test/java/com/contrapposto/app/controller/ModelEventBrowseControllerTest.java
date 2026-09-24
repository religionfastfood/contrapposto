package com.contrapposto.app.controller;

import com.contrapposto.app.config.SecurityConfig;
import com.contrapposto.app.model.Event;
import com.contrapposto.app.model.EventType;
import com.contrapposto.app.model.ModelProfile;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.User;
import com.contrapposto.app.security.CustomUserDetailsService;
import com.contrapposto.app.security.FormLoginSuccessHandler;
import com.contrapposto.app.security.OAuth2AuthenticationSuccessHandler;
import com.contrapposto.app.security.UserPrincipal;
import com.contrapposto.app.service.EventApplicationService;
import com.contrapposto.app.service.EventService;
import com.contrapposto.app.service.ModelProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ModelEventBrowseController.class)
@Import(SecurityConfig.class)
class ModelEventBrowseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private ModelProfileService modelProfileService;

    @MockitoBean
    private EventApplicationService eventApplicationService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private FormLoginSuccessHandler formLoginSuccessHandler;

    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    private User modelUser() {
        return User.builder().id(2L).email("model@example.com").role(Role.MODEL).enabled(true).build();
    }

    private ModelProfile profileWithCity(String city) {
        ModelProfile profile = new ModelProfile(modelUser());
        profile.setCity(city);
        return profile;
    }

    private Event eventIn(String city) {
        User organizer = User.builder().id(1L).email("organizer@example.com").role(Role.ORGANIZER).build();
        Event event = new Event(organizer);
        event.setId(1L);
        event.setEventType(new EventType("Gesture"));
        event.setTitle("Gesture Night");
        event.setCity(city);
        event.setLocation("123 Main St");
        event.setStartTime(LocalDateTime.now().plusDays(1));
        return event;
    }

    @Test
    void browse_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/model/events"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void browse_wrongRole_isForbidden() throws Exception {
        User organizer = User.builder().id(1L).email("organizer@example.com").role(Role.ORGANIZER).enabled(true).build();
        mockMvc.perform(get("/model/events").with(user(new UserPrincipal(organizer))))
                .andExpect(status().isForbidden());
    }

    @Test
    void browse_noCityParam_defaultsToProfileCity() throws Exception {
        when(modelProfileService.getOrCreateProfile(any())).thenReturn(profileWithCity("Portland"));
        when(eventService.upcomingByCity("Portland")).thenReturn(List.of(eventIn("Portland")));

        mockMvc.perform(get("/model/events").with(user(new UserPrincipal(modelUser()))))
                .andExpect(status().isOk())
                .andExpect(view().name("model/events"))
                .andExpect(model().attribute("city", "Portland"))
                .andExpect(model().attributeExists("events"))
                .andExpect(model().attributeExists("assignedModels"));
    }

    @Test
    void browse_noCityParamAndNoProfileCity_showsPromptWithoutQueryingEvents() throws Exception {
        when(modelProfileService.getOrCreateProfile(any())).thenReturn(profileWithCity(null));

        mockMvc.perform(get("/model/events").with(user(new UserPrincipal(modelUser()))))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("events"));
        verify(eventService, never()).upcomingByCity(any());
    }

    @Test
    void browse_explicitCityParam_overridesProfileDefault() throws Exception {
        when(modelProfileService.getOrCreateProfile(any())).thenReturn(profileWithCity("Portland"));
        when(eventService.upcomingByCity("Austin")).thenReturn(List.of(eventIn("Austin")));

        mockMvc.perform(get("/model/events").param("city", "Austin").with(user(new UserPrincipal(modelUser()))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("city", "Austin"))
                .andExpect(model().attributeExists("events"));
        verify(eventService, never()).upcomingByCity("Portland");
    }

    @Test
    void browse_explicitBlankCityParam_clearsProfileDefaultRatherThanReapplyingIt() throws Exception {
        when(modelProfileService.getOrCreateProfile(any())).thenReturn(profileWithCity("Portland"));

        mockMvc.perform(get("/model/events").param("city", "").with(user(new UserPrincipal(modelUser()))))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("events"));
        verify(eventService, never()).upcomingByCity(any());
    }
}
