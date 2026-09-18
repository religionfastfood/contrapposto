package com.contrapposto.app.controller;

import com.contrapposto.app.dto.OrganizerProfileRequest;
import com.contrapposto.app.model.OrganizerProfile;
import com.contrapposto.app.security.UserPrincipal;
import com.contrapposto.app.service.OrganizerProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/organizer/profile")
public class OrganizerProfileController {

    private final OrganizerProfileService organizerProfileService;

    public OrganizerProfileController(OrganizerProfileService organizerProfileService) {
        this.organizerProfileService = organizerProfileService;
    }

    @GetMapping
    public String view(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        OrganizerProfile profile = organizerProfileService.getOrCreateProfile(principal.getUser());
        model.addAttribute("profile", profile);
        if (!model.containsAttribute("profileRequest")) {
            model.addAttribute("profileRequest", requestFrom(profile));
        }
        return "organizer/profile";
    }

    @PostMapping
    public String update(@Valid @ModelAttribute("profileRequest") OrganizerProfileRequest request,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal UserPrincipal principal,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("profile", organizerProfileService.getOrCreateProfile(principal.getUser()));
            return "organizer/profile";
        }
        organizerProfileService.updateProfile(principal.getUser(), request);
        model.addAttribute("success", "Profile updated.");
        model.addAttribute("profile", organizerProfileService.getOrCreateProfile(principal.getUser()));
        return "organizer/profile";
    }

    private OrganizerProfileRequest requestFrom(OrganizerProfile profile) {
        OrganizerProfileRequest request = new OrganizerProfileRequest();
        request.setDisplayName(profile.getDisplayName());
        request.setOrgInfo(profile.getOrgInfo());
        request.setCity(profile.getCity());
        return request;
    }
}
