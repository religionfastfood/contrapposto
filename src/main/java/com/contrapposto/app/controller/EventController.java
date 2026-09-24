package com.contrapposto.app.controller;

import com.contrapposto.app.dto.EventRequest;
import com.contrapposto.app.model.Event;
import com.contrapposto.app.security.UserPrincipal;
import com.contrapposto.app.service.EventApplicationService;
import com.contrapposto.app.service.EventService;
import com.contrapposto.app.service.EventTypeService;
import com.contrapposto.app.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/organizer/events")
public class EventController {

    private final EventService eventService;
    private final EventTypeService eventTypeService;
    private final SubscriptionService subscriptionService;
    private final EventApplicationService eventApplicationService;

    public EventController(EventService eventService, EventTypeService eventTypeService,
                            SubscriptionService subscriptionService, EventApplicationService eventApplicationService) {
        this.eventService = eventService;
        this.eventTypeService = eventTypeService;
        this.subscriptionService = subscriptionService;
        this.eventApplicationService = eventApplicationService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        List<Event> events = eventService.findByOrganizer(principal.getUser());
        model.addAttribute("events", events);
        model.addAttribute("assignedModels", eventApplicationService.findAssignedModels(events));
        model.addAttribute("now", LocalDateTime.now());
        return "organizer/events";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal UserPrincipal principal, Model model,
                           RedirectAttributes redirectAttributes) {
        if (!subscriptionService.isSubscriptionActive(principal.getUser())) {
            redirectAttributes.addFlashAttribute("error", "An active subscription is required to post new events.");
            return "redirect:/organizer/events";
        }
        model.addAttribute("eventTypes", eventTypeService.findAll());
        if (!model.containsAttribute("eventRequest")) {
            model.addAttribute("eventRequest", new EventRequest());
        }
        return "organizer/event-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("eventRequest") EventRequest request,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal UserPrincipal principal,
                         Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("eventTypes", eventTypeService.findAll());
            return "organizer/event-form";
        }
        try {
            eventService.create(principal.getUser(), request);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/organizer/events";
        }
        redirectAttributes.addFlashAttribute("success", "Event posted.");
        return "redirect:/organizer/events";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal, Model model) {
        Event event = eventService.findOwnedById(principal.getUser(), id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        model.addAttribute("eventTypes", eventTypeService.findAll());
        if (!model.containsAttribute("eventRequest")) {
            model.addAttribute("eventRequest", requestFrom(event));
        }
        model.addAttribute("eventId", id);
        return "organizer/event-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("eventRequest") EventRequest request,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal UserPrincipal principal,
                         Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("eventTypes", eventTypeService.findAll());
            model.addAttribute("eventId", id);
            return "organizer/event-form";
        }
        eventService.update(principal.getUser(), id, request)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        redirectAttributes.addFlashAttribute("success", "Event updated.");
        return "redirect:/organizer/events";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        if (!eventService.delete(principal.getUser(), id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
        }
        redirectAttributes.addFlashAttribute("success", "Event deleted.");
        return "redirect:/organizer/events";
    }

    private EventRequest requestFrom(Event event) {
        EventRequest request = new EventRequest();
        request.setEventTypeId(event.getEventType().getId());
        request.setTitle(event.getTitle());
        request.setDescription(event.getDescription());
        request.setCity(event.getCity());
        request.setLocation(event.getLocation());
        request.setStartTime(event.getStartTime());
        request.setPriceAmount(event.getPriceAmount());
        request.setExternalLink(event.getExternalLink());
        return request;
    }
}
