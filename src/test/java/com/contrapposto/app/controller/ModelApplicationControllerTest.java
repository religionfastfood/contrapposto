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

@WebMvcTest(ModelApplicationController.class)
@Import(SecurityConfig.class)
class ModelApplicationControllerTest {

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

    private User modelUser() {
        return User.builder().id(2L).email("model@example.com").role(Role.MODEL).enabled(true).build();
    }

    private User organizerUser() {
        return User.builder().id(1L).email("organizer@example.com").role(Role.ORGANIZER).enabled(true).build();
    }

    private EventApplication application(Long id) {
        Event event = new Event(organizerUser());
        event.setId(10L);
        event.setEventType(new EventType("Gesture"));
        EventApplication application = new EventApplication(event, modelUser(), ApplicationInitiator.ORGANIZER, null);
        application.setId(id);
        return application;
    }

    @Test
    void list_asModel_returnsOk() throws Exception {
        when(eventApplicationService.findForModel(any())).thenReturn(List.of(application(1L)));

        mockMvc.perform(get("/model/applications").with(user(new UserPrincipal(modelUser()))))
                .andExpect(status().isOk())
                .andExpect(view().name("model/applications"))
                .andExpect(model().attributeExists("applications"));
    }

    @Test
    void list_wrongRole_isForbidden() throws Exception {
        mockMvc.perform(get("/model/applications").with(user(new UserPrincipal(organizerUser()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void apply_success_redirectsWithSuccessFlash() throws Exception {
        when(eventApplicationService.apply(any(), eq(10L), any())).thenReturn(application(1L));

        mockMvc.perform(post("/model/events/10/apply").with(user(new UserPrincipal(modelUser()))).with(csrf())
                        .param("message", "Looking forward to it"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/10"));
    }

    @Test
    void apply_serviceRejects_redirectsWithErrorFlash() throws Exception {
        when(eventApplicationService.apply(any(), eq(10L), any()))
                .thenThrow(new IllegalStateException("An active subscription is required"));

        mockMvc.perform(post("/model/events/10/apply").with(user(new UserPrincipal(modelUser()))).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/10"));
    }

    @Test
    void accept_found_redirectsToInbox() throws Exception {
        when(eventApplicationService.accept(any(), eq(1L))).thenReturn(Optional.of(application(1L)));

        mockMvc.perform(post("/model/applications/1/accept").with(user(new UserPrincipal(modelUser()))).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/model/applications"));
    }

    @Test
    void accept_notFoundOrNotEntitled_returnsNotFound() throws Exception {
        when(eventApplicationService.accept(any(), eq(999L))).thenReturn(Optional.empty());

        mockMvc.perform(post("/model/applications/999/accept").with(user(new UserPrincipal(modelUser()))).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void accept_eventAlreadyAssigned_redirectsWithErrorFlashInsteadOfPropagating() throws Exception {
        when(eventApplicationService.accept(any(), eq(1L)))
                .thenThrow(new IllegalStateException("This event already has an assigned model"));

        mockMvc.perform(post("/model/applications/1/accept").with(user(new UserPrincipal(modelUser()))).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/model/applications"));
    }

    @Test
    void decline_found_redirectsToInbox() throws Exception {
        when(eventApplicationService.decline(any(), eq(1L))).thenReturn(Optional.of(application(1L)));

        mockMvc.perform(post("/model/applications/1/decline").with(user(new UserPrincipal(modelUser()))).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/model/applications"));
    }

    @Test
    void withdraw_found_redirectsToInbox() throws Exception {
        when(eventApplicationService.withdraw(any(), eq(1L))).thenReturn(Optional.of(application(1L)));

        mockMvc.perform(post("/model/applications/1/withdraw").with(user(new UserPrincipal(modelUser()))).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/model/applications"));
    }
}
