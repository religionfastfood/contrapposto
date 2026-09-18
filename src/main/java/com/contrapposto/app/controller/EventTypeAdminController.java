package com.contrapposto.app.controller;

import com.contrapposto.app.dto.EventTypeRequest;
import com.contrapposto.app.service.EventTypeService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/event-types")
public class EventTypeAdminController {

    private final EventTypeService eventTypeService;

    public EventTypeAdminController(EventTypeService eventTypeService) {
        this.eventTypeService = eventTypeService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("eventTypes", eventTypeService.findAll());
        if (!model.containsAttribute("eventTypeRequest")) {
            model.addAttribute("eventTypeRequest", new EventTypeRequest());
        }
        return "admin/event-types";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("eventTypeRequest") EventTypeRequest request,
                         BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("eventTypes", eventTypeService.findAll());
            return "admin/event-types";
        }
        try {
            eventTypeService.create(request.getName());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/event-types";
        }
        redirectAttributes.addFlashAttribute("success", "Event type added.");
        return "redirect:/admin/event-types";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            eventTypeService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Event type deleted.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/event-types";
    }
}
