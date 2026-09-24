package com.contrapposto.app.controller;

import com.contrapposto.app.model.Event;
import com.contrapposto.app.service.EventApplicationService;
import com.contrapposto.app.service.EventService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class HomeController {

    private final EventService eventService;
    private final EventApplicationService eventApplicationService;

    public HomeController(EventService eventService, EventApplicationService eventApplicationService) {
        this.eventService = eventService;
        this.eventApplicationService = eventApplicationService;
    }

    @GetMapping("/")
    public String index(@RequestParam(required = false) String city, Model model) {
        if (StringUtils.hasText(city)) {
            List<Event> events = eventService.upcomingByCity(city.trim());
            model.addAttribute("events", events);
            model.addAttribute("assignedModels", eventApplicationService.findAssignedModels(events));
        }
        return "index";
    }
}