package com.contrapposto.app.controller;

import com.contrapposto.app.config.SecurityConfig;
import com.contrapposto.app.model.ApplicationInitiator;
import com.contrapposto.app.model.Event;
import com.contrapposto.app.model.EventApplication;
import com.contrapposto.app.model.EventType;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.User;
import com.contrapposto.app.security.CustomUserDetailsService;
import com.contrapposto.app.security.FormLoginSuccessHandler;
import com.contrapposto.app.security.OAuth2AuthenticationSuccessHandler;
import com.contrapposto.app.security.UserPrincipal;
import com.contrapposto.app.service.EventApplicationService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrganizerApplicationController.class)
@Import(SecurityConfig.class)
class OrganizerApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventApplicationService eventApplicationService;

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

    private EventApplication application(Long id) {
        Event event = new Event(organizerUser());
        event.setId(10L);
        event.setEventType(new EventType("Gesture"));
        EventApplication application = new EventApplication(event, modelUser(), ApplicationInitiator.MODEL, null);
        application.setId(id);
        return application;
    }

    @Test
    void list_asOrganizer_returnsOk() throws Exception {
        when(eventApplicationService.findForOrganizer(any())).thenReturn(List.of(application(1L)));

        mockMvc.perform(get("/organizer/applications").with(user(new UserPrincipal(organizerUser()))))
                .andExpect(status().isOk())
                .andExpect(view().name("organizer/applications"))
                .andExpect(model().attributeExists("applications"))
                .andExpect(model().attributeExists("priorDeclines"));
    }

    @Test
    void list_wrongRole_isForbidden() throws Exception {
        mockMvc.perform(get("/organizer/applications").with(user(new UserPrincipal(modelUser()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void invite_success_redirectsToModelDetailWithSuccessFlash() throws Exception {
        when(eventApplicationService.invite(any(), eq(10L), eq(2L), any())).thenReturn(application(1L));

        mockMvc.perform(post("/organizer/models/2/invite").with(user(new UserPrincipal(organizerUser()))).with(csrf())
                        .param("eventId", "10")
                        .param("message", "Would love to have you"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/organizer/models/2"));
    }

    @Test
    void invite_missingEventId_redirectsWithErrorFlash() throws Exception {
        mockMvc.perform(post("/organizer/models/2/invite").with(user(new UserPrincipal(organizerUser()))).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/organizer/models/2"));
    }

    @Test
    void invite_serviceRejects_redirectsWithErrorFlash() throws Exception {
        when(eventApplicationService.invite(any(), eq(10L), eq(2L), any()))
                .thenThrow(new IllegalStateException("An active application or invitation already exists"));

        mockMvc.perform(post("/organizer/models/2/invite").with(user(new UserPrincipal(organizerUser()))).with(csrf())
                        .param("eventId", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/organizer/models/2"));
    }

    @Test
    void accept_found_redirectsToInbox() throws Exception {
        when(eventApplicationService.accept(any(), eq(1L))).thenReturn(Optional.of(application(1L)));

        mockMvc.perform(post("/organizer/applications/1/accept").with(user(new UserPrincipal(organizerUser()))).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/organizer/applications"));
    }

    @Test
    void accept_eventAlreadyAssigned_redirectsWithErrorFlashInsteadOfPropagating() throws Exception {
        when(eventApplicationService.accept(any(), eq(1L)))
                .thenThrow(new IllegalStateException("This event already has an assigned model"));

        mockMvc.perform(post("/organizer/applications/1/accept").with(user(new UserPrincipal(organizerUser()))).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/organizer/applications"));
    }

    @Test
    void decline_notFoundOrNotEntitled_returnsNotFound() throws Exception {
        when(eventApplicationService.decline(any(), eq(999L))).thenReturn(Optional.empty());

        mockMvc.perform(post("/organizer/applications/999/decline").with(user(new UserPrincipal(organizerUser()))).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void withdraw_found_redirectsToInbox() throws Exception {
        when(eventApplicationService.withdraw(any(), eq(1L))).thenReturn(Optional.of(application(1L)));

        mockMvc.perform(post("/organizer/applications/1/withdraw").with(user(new UserPrincipal(organizerUser()))).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/organizer/applications"));
    }
}
