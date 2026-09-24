package com.contrapposto.app.controller;

import com.contrapposto.app.config.SecurityConfig;
import com.contrapposto.app.model.Event;
import com.contrapposto.app.model.EventType;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.User;
import com.contrapposto.app.security.CustomUserDetailsService;
import com.contrapposto.app.security.FormLoginSuccessHandler;
import com.contrapposto.app.security.OAuth2AuthenticationSuccessHandler;
import com.contrapposto.app.security.UserPrincipal;
import com.contrapposto.app.service.EventApplicationService;
import com.contrapposto.app.service.EventService;
import com.contrapposto.app.service.OrganizerProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventDetailController.class)
@Import(SecurityConfig.class)
class EventDetailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private OrganizerProfileService organizerProfileService;

    @MockitoBean
    private EventApplicationService eventApplicationService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private FormLoginSuccessHandler formLoginSuccessHandler;

    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Test
    void view_existingEvent_isPubliclyAccessibleWithoutAuth() throws Exception {
        User organizer = User.builder().id(1L).email("organizer@example.com").role(Role.ORGANIZER).build();
        Event event = new Event(organizer);
        event.setId(5L);
        event.setEventType(new EventType("Gesture"));
        event.setTitle("Gesture Night");
        event.setCity("Portland");
        event.setLocation("123 Main St");
        event.setStartTime(LocalDateTime.now().plusDays(1));
        when(eventService.findById(5L)).thenReturn(Optional.of(event));
        when(organizerProfileService.findByUser(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/events/5"))
                .andExpect(status().isOk())
                .andExpect(view().name("events/detail"))
                .andExpect(model().attributeExists("event"));
    }

    @Test
    void view_nonExistentEvent_returnsNotFound() throws Exception {
        when(eventService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/events/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void view_asModel_addsExistingApplicationAttribute() throws Exception {
        User organizer = User.builder().id(1L).email("organizer@example.com").role(Role.ORGANIZER).build();
        Event event = new Event(organizer);
        event.setId(5L);
        event.setEventType(new EventType("Gesture"));
        event.setTitle("Gesture Night");
        event.setCity("Portland");
        event.setLocation("123 Main St");
        event.setStartTime(LocalDateTime.now().plusDays(1));
        when(eventService.findById(5L)).thenReturn(Optional.of(event));
        when(organizerProfileService.findByUser(any())).thenReturn(Optional.empty());

        User model = User.builder().id(2L).email("model@example.com").role(Role.MODEL).enabled(true).build();
        com.contrapposto.app.model.EventApplication application = new com.contrapposto.app.model.EventApplication(
                event, model, com.contrapposto.app.model.ApplicationInitiator.MODEL, null);
        when(eventApplicationService.findActiveForEventAndModel(any(), any())).thenReturn(Optional.of(application));

        mockMvc.perform(get("/events/5").with(user(new UserPrincipal(model))))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("existingApplication"));
    }

    @Test
    void view_noAcceptedModel_assignedModelAttributeIsAbsentValue() throws Exception {
        User organizer = User.builder().id(1L).email("organizer@example.com").role(Role.ORGANIZER).build();
        Event event = new Event(organizer);
        event.setId(5L);
        event.setEventType(new EventType("Gesture"));
        event.setTitle("Gesture Night");
        event.setCity("Portland");
        event.setLocation("123 Main St");
        event.setStartTime(LocalDateTime.now().plusDays(1));
        when(eventService.findById(5L)).thenReturn(Optional.of(event));
        when(organizerProfileService.findByUser(any())).thenReturn(Optional.empty());
        when(eventApplicationService.findAssignedModel(event)).thenReturn(Optional.empty());

        mockMvc.perform(get("/events/5"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("assignedModel", (Object) null));
    }

    @Test
    void view_acceptedModel_assignedModelAttributePopulated() throws Exception {
        User organizer = User.builder().id(1L).email("organizer@example.com").role(Role.ORGANIZER).build();
        Event event = new Event(organizer);
        event.setId(5L);
        event.setEventType(new EventType("Gesture"));
        event.setTitle("Gesture Night");
        event.setCity("Portland");
        event.setLocation("123 Main St");
        event.setStartTime(LocalDateTime.now().plusDays(1));
        when(eventService.findById(5L)).thenReturn(Optional.of(event));
        when(organizerProfileService.findByUser(any())).thenReturn(Optional.empty());
        com.contrapposto.app.service.AssignedModelView view =
                new com.contrapposto.app.service.AssignedModelView("Ava", "https://example.com/ava.jpg");
        when(eventApplicationService.findAssignedModel(event)).thenReturn(Optional.of(view));

        mockMvc.perform(get("/events/5"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("assignedModel", view));
    }
}
