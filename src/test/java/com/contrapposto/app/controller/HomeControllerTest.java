package com.contrapposto.app.controller;

import com.contrapposto.app.config.SecurityConfig;
import com.contrapposto.app.model.Event;
import com.contrapposto.app.model.EventType;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.User;
import com.contrapposto.app.security.CustomUserDetailsService;
import com.contrapposto.app.security.FormLoginSuccessHandler;
import com.contrapposto.app.security.OAuth2AuthenticationSuccessHandler;
import com.contrapposto.app.service.EventApplicationService;
import com.contrapposto.app.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
@Import(SecurityConfig.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private EventApplicationService eventApplicationService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private FormLoginSuccessHandler formLoginSuccessHandler;

    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Test
    void home_noCity_returnsOkWithoutQueryingEvents() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeDoesNotExist("events"));
        verify(eventService, never()).upcomingByCity(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void home_withCity_populatesEventsAttribute() throws Exception {
        User organizer = User.builder().id(1L).email("organizer@example.com").role(Role.ORGANIZER).build();
        Event event = new Event(organizer);
        event.setId(1L);
        event.setEventType(new EventType("Gesture"));
        event.setTitle("Gesture Night");
        event.setCity("Portland");
        event.setLocation("123 Main St");
        event.setStartTime(LocalDateTime.now().plusDays(1));
        when(eventService.upcomingByCity("Portland")).thenReturn(List.of(event));

        mockMvc.perform(get("/").param("city", "Portland"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("events"))
                .andExpect(model().attributeExists("assignedModels"));
    }

    @Test
    void home_blankCity_treatedAsNoFilter() throws Exception {
        mockMvc.perform(get("/").param("city", "   "))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("events"));
    }
}
