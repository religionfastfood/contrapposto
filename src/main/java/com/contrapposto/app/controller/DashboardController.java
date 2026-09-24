package com.contrapposto.app.controller;

import com.contrapposto.app.model.ModelProfile;
import com.contrapposto.app.model.OrganizerProfile;
import com.contrapposto.app.security.UserPrincipal;
import com.contrapposto.app.service.ModelProfileService;
import com.contrapposto.app.service.OrganizerProfileService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
public class DashboardController {

    private final OrganizerProfileService organizerProfileService;
    private final ModelProfileService modelProfileService;

    public DashboardController(OrganizerProfileService organizerProfileService, ModelProfileService modelProfileService) {
        this.organizerProfileService = organizerProfileService;
        this.modelProfileService = modelProfileService;
    }

    @GetMapping("/organizer/dashboard")
    @PreAuthorize("hasRole('ORGANIZER')")
    public String organizerDashboard(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        Optional<OrganizerProfile> profile = organizerProfileService.findByUser(principal.getUser());
        profile.ifPresent(organizerProfile -> model.addAttribute("name", organizerProfile.getDisplayName()));
        return "organizer/dashboard";
    }

    @GetMapping("/model/dashboard")
    @PreAuthorize("hasRole('MODEL')")
    public String modelDashboard(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        Optional<ModelProfile> profile = modelProfileService.findByUser(principal.getUser());
        profile.ifPresent(modelProfile -> model.addAttribute("name", modelProfile.getDisplayName()));
        return "model/dashboard";
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard() {
        return "admin/dashboard";
    }
}