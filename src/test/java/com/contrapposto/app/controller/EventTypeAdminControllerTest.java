package com.contrapposto.app.controller;

import com.contrapposto.app.config.SecurityConfig;
import com.contrapposto.app.model.EventType;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.User;
import com.contrapposto.app.security.CustomUserDetailsService;
import com.contrapposto.app.security.FormLoginSuccessHandler;
import com.contrapposto.app.security.OAuth2AuthenticationSuccessHandler;
import com.contrapposto.app.security.UserPrincipal;
import com.contrapposto.app.service.EventTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventTypeAdminController.class)
@Import(SecurityConfig.class)
class EventTypeAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventTypeService eventTypeService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private FormLoginSuccessHandler formLoginSuccessHandler;

    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    private UserPrincipal adminPrincipal() {
        return new UserPrincipal(User.builder().id(1L).email("admin@example.com").role(Role.ADMIN).enabled(true).build());
    }

    private UserPrincipal organizerPrincipal() {
        return new UserPrincipal(User.builder().id(2L).email("organizer@example.com").role(Role.ORGANIZER).enabled(true).build());
    }

    @Test
    void list_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/event-types"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void list_wrongRole_isForbidden() throws Exception {
        mockMvc.perform(get("/admin/event-types").with(user(organizerPrincipal())))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_asAdmin_returnsOk() throws Exception {
        when(eventTypeService.findAll()).thenReturn(List.of(new EventType("Gesture")));

        mockMvc.perform(get("/admin/event-types").with(user(adminPrincipal())))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/event-types"))
                .andExpect(model().attributeExists("eventTypes", "eventTypeRequest"));
    }

    @Test
    void create_validName_redirectsWithSuccess() throws Exception {
        when(eventTypeService.create("Croquis")).thenReturn(new EventType("Croquis"));

        mockMvc.perform(post("/admin/event-types").with(csrf()).with(user(adminPrincipal()))
                        .param("name", "Croquis"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/event-types"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void create_duplicateName_redirectsWithError() throws Exception {
        when(eventTypeService.create(any())).thenThrow(new IllegalArgumentException("already exists"));

        mockMvc.perform(post("/admin/event-types").with(csrf()).with(user(adminPrincipal()))
                        .param("name", "Gesture"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "already exists"));
    }

    @Test
    void create_blankName_returnsFormWithFieldError() throws Exception {
        when(eventTypeService.findAll()).thenReturn(List.of());

        mockMvc.perform(post("/admin/event-types").with(csrf()).with(user(adminPrincipal()))
                        .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/event-types"))
                .andExpect(model().attributeHasFieldErrors("eventTypeRequest", "name"));
    }

    @Test
    void delete_notInUse_redirectsWithSuccess() throws Exception {
        mockMvc.perform(post("/admin/event-types/1/delete").with(csrf()).with(user(adminPrincipal())))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void delete_inUse_redirectsWithError() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalStateException("still used"))
                .when(eventTypeService).delete(eq(1L));

        mockMvc.perform(post("/admin/event-types/1/delete").with(csrf()).with(user(adminPrincipal())))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "still used"));
    }
}
