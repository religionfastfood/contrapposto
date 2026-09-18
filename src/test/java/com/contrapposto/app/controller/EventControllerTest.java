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
import com.contrapposto.app.service.EventService;
import com.contrapposto.app.service.EventTypeService;
import com.contrapposto.app.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
@Import(SecurityConfig.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private EventTypeService eventTypeService;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private FormLoginSuccessHandler formLoginSuccessHandler;

    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    private User organizerUser() {
        return User.builder().id(1L).email("organizer@example.com").role(Role.ORGANIZER).enabled(true).build();
    }

    private UserPrincipal modelPrincipal() {
        User user = User.builder().id(2L).email("model@example.com").role(Role.MODEL).enabled(true).build();
        return new UserPrincipal(user);
    }

    private Event sampleEvent(User organizer) {
        Event event = new Event(organizer);
        event.setId(5L);
        event.setEventType(new EventType("Gesture"));
        event.setTitle("Gesture Night");
        event.setCity("Portland");
        event.setLocation("123 Main St");
        event.setStartTime(LocalDateTime.now().plusDays(1));
        return event;
    }

    // --- access control ---

    @Test
    void list_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/organizer/events"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void list_wrongRole_isForbidden() throws Exception {
        mockMvc.perform(get("/organizer/events").with(user(modelPrincipal())))
                .andExpect(status().isForbidden());
    }

    // --- list ---

    @Test
    void list_asOrganizer_returnsOwnEvents() throws Exception {
        User organizer = organizerUser();
        when(eventService.findByOrganizer(any())).thenReturn(List.of(sampleEvent(organizer)));

        mockMvc.perform(get("/organizer/events").with(user(new UserPrincipal(organizer))))
                .andExpect(status().isOk())
                .andExpect(view().name("organizer/events"))
                .andExpect(model().attributeExists("events", "now"));
    }

    // --- new form: subscription gating ---

    @Test
    void newForm_activeSubscription_showsForm() throws Exception {
        User organizer = organizerUser();
        when(subscriptionService.isSubscriptionActive(any())).thenReturn(true);
        when(eventTypeService.findAll()).thenReturn(List.of(new EventType("Gesture")));

        mockMvc.perform(get("/organizer/events/new").with(user(new UserPrincipal(organizer))))
                .andExpect(status().isOk())
                .andExpect(view().name("organizer/event-form"))
                .andExpect(model().attributeExists("eventTypes", "eventRequest"));
    }

    @Test
    void newForm_inactiveSubscription_redirectsWithError() throws Exception {
        User organizer = organizerUser();
        when(subscriptionService.isSubscriptionActive(any())).thenReturn(false);

        mockMvc.perform(get("/organizer/events/new").with(user(new UserPrincipal(organizer))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/organizer/events"))
                .andExpect(flash().attributeExists("error"));
    }

    // --- create ---

    @Test
    void create_validData_redirectsWithSuccess() throws Exception {
        User organizer = organizerUser();
        when(eventService.create(any(), any())).thenReturn(sampleEvent(organizer));

        mockMvc.perform(post("/organizer/events").with(csrf()).with(user(new UserPrincipal(organizer)))
                        .param("eventTypeId", "1")
                        .param("title", "Gesture Night")
                        .param("city", "Portland")
                        .param("location", "123 Main St")
                        .param("startTime", "2027-01-01T18:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/organizer/events"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void create_missingTitle_returnsFormWithFieldError() throws Exception {
        User organizer = organizerUser();
        when(eventTypeService.findAll()).thenReturn(List.of(new EventType("Gesture")));

        mockMvc.perform(post("/organizer/events").with(csrf()).with(user(new UserPrincipal(organizer)))
                        .param("eventTypeId", "1")
                        .param("city", "Portland")
                        .param("location", "123 Main St")
                        .param("startTime", "2027-01-01T18:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("organizer/event-form"))
                .andExpect(model().attributeHasFieldErrors("eventRequest", "title"));
    }

    @Test
    void create_badExternalLinkScheme_returnsFormWithFieldError() throws Exception {
        User organizer = organizerUser();
        when(eventTypeService.findAll()).thenReturn(List.of(new EventType("Gesture")));

        mockMvc.perform(post("/organizer/events").with(csrf()).with(user(new UserPrincipal(organizer)))
                        .param("eventTypeId", "1")
                        .param("title", "Gesture Night")
                        .param("city", "Portland")
                        .param("location", "123 Main St")
                        .param("startTime", "2027-01-01T18:00")
                        .param("externalLink", "javascript:alert(1)"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("eventRequest", "externalLink"));
    }

    @Test
    void create_serviceRejectsInactiveSubscription_redirectsWithError() throws Exception {
        User organizer = organizerUser();
        when(eventService.create(any(), any())).thenThrow(new IllegalStateException("nope"));

        mockMvc.perform(post("/organizer/events").with(csrf()).with(user(new UserPrincipal(organizer)))
                        .param("eventTypeId", "1")
                        .param("title", "Gesture Night")
                        .param("city", "Portland")
                        .param("location", "123 Main St")
                        .param("startTime", "2027-01-01T18:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/organizer/events"))
                .andExpect(flash().attribute("error", "nope"));
    }

    // --- edit/update/delete: ownership -> 404 ---

    @Test
    void editForm_ownedEvent_returnsOk() throws Exception {
        User organizer = organizerUser();
        when(eventService.findOwnedById(any(), eq(5L))).thenReturn(Optional.of(sampleEvent(organizer)));
        when(eventTypeService.findAll()).thenReturn(List.of(new EventType("Gesture")));

        mockMvc.perform(get("/organizer/events/5/edit").with(user(new UserPrincipal(organizer))))
                .andExpect(status().isOk())
                .andExpect(view().name("organizer/event-form"));
    }

    @Test
    void editForm_notOwned_returnsNotFound() throws Exception {
        when(eventService.findOwnedById(any(), eq(5L))).thenReturn(Optional.empty());

        mockMvc.perform(get("/organizer/events/5/edit").with(user(new UserPrincipal(organizerUser()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_notOwned_returnsNotFound() throws Exception {
        when(eventService.update(any(), eq(5L), any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/organizer/events/5").with(csrf()).with(user(new UserPrincipal(organizerUser())))
                        .param("eventTypeId", "1")
                        .param("title", "Gesture Night")
                        .param("city", "Portland")
                        .param("location", "123 Main St")
                        .param("startTime", "2027-01-01T18:00"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_notOwned_returnsNotFound() throws Exception {
        when(eventService.delete(any(), eq(5L))).thenReturn(false);

        mockMvc.perform(post("/organizer/events/5/delete").with(csrf()).with(user(new UserPrincipal(organizerUser()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_owned_redirectsWithSuccess() throws Exception {
        when(eventService.delete(any(), eq(5L))).thenReturn(true);

        mockMvc.perform(post("/organizer/events/5/delete").with(csrf()).with(user(new UserPrincipal(organizerUser()))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/organizer/events"))
                .andExpect(flash().attributeExists("success"));
    }
}
