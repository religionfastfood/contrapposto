package com.contrapposto.app.controller;

import com.contrapposto.app.model.Event;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.security.UserPrincipal;
import com.contrapposto.app.service.EventApplicationService;
import com.contrapposto.app.service.EventService;
import com.contrapposto.app.service.OrganizerProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class EventDetailController {

    private final EventService eventService;
    private final OrganizerProfileService organizerProfileService;
    private final EventApplicationService eventApplicationService;

    public EventDetailController(EventService eventService, OrganizerProfileService organizerProfileService,
                                  EventApplicationService eventApplicationService) {
        this.eventService = eventService;
        this.organizerProfileService = organizerProfileService;
        this.eventApplicationService = eventApplicationService;
    }

    @GetMapping("/events/{id}")
    public String view(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal, Model model) {
        Event event = eventService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        model.addAttribute("event", event);
        model.addAttribute("organizerProfile", organizerProfileService.findByUser(event.getOrganizer()).orElse(null));
        if (principal != null && principal.getUser().getRole() == Role.MODEL) {
            model.addAttribute("existingApplication",
                    eventApplicationService.findActiveForEventAndModel(event, principal.getUser()).orElse(null));
        }
        return "events/detail";
    }
}
