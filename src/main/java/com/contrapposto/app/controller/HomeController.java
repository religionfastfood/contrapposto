package com.contrapposto.app.controller;

import com.contrapposto.app.service.EventService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    private final EventService eventService;

    public HomeController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/")
    public String index(@RequestParam(required = false) String city, Model model) {
        if (StringUtils.hasText(city)) {
            model.addAttribute("events", eventService.upcomingByCity(city.trim()));
        }
        return "index";
    }
}