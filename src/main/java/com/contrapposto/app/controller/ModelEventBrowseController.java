package com.contrapposto.app.controller;

import com.contrapposto.app.security.UserPrincipal;
import com.contrapposto.app.service.EventService;
import com.contrapposto.app.service.ModelProfileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ModelEventBrowseController {

    private final EventService eventService;
    private final ModelProfileService modelProfileService;

    public ModelEventBrowseController(EventService eventService, ModelProfileService modelProfileService) {
        this.eventService = eventService;
        this.modelProfileService = modelProfileService;
    }

    @GetMapping("/model/events")
    public String browse(@RequestParam(required = false) String city,
                          @AuthenticationPrincipal UserPrincipal principal, Model model) {
        // No city param on the initial visit -- default to the model's own profile city, but let
        // an explicit ?city= (even blank, e.g. a cleared search box) override it, rather than
        // stubbornly reapplying the profile default after the model has already changed the filter.
        String effectiveCity = city != null ? city : modelProfileService.getOrCreateProfile(principal.getUser()).getCity();
        if (StringUtils.hasText(effectiveCity)) {
            model.addAttribute("events", eventService.upcomingByCity(effectiveCity.trim()));
        }
        model.addAttribute("city", effectiveCity);
        return "model/events";
    }
}
