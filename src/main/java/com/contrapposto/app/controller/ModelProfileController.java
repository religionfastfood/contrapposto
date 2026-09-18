package com.contrapposto.app.controller;

import com.contrapposto.app.dto.ModelProfileRequest;
import com.contrapposto.app.model.ModelProfile;
import com.contrapposto.app.model.User;
import com.contrapposto.app.security.UserPrincipal;
import com.contrapposto.app.service.ModelProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/model/profile")
public class ModelProfileController {

    private final ModelProfileService modelProfileService;

    public ModelProfileController(ModelProfileService modelProfileService) {
        this.modelProfileService = modelProfileService;
    }

    @GetMapping
    public String view(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        ModelProfile profile = modelProfileService.getOrCreateProfile(principal.getUser());
        model.addAttribute("profile", profile);
        if (!model.containsAttribute("profileRequest")) {
            model.addAttribute("profileRequest", requestFrom(profile));
        }
        return "model/profile";
    }

    @PostMapping
    public String update(@Valid @ModelAttribute("profileRequest") ModelProfileRequest request,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal UserPrincipal principal,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("profile", modelProfileService.getOrCreateProfile(principal.getUser()));
            return "model/profile";
        }
        modelProfileService.updateProfile(principal.getUser(), request);
        model.addAttribute("success", "Profile updated.");
        model.addAttribute("profile", modelProfileService.getOrCreateProfile(principal.getUser()));
        return "model/profile";
    }

    @PostMapping("/photos")
    public String addPhoto(@RequestParam("file") MultipartFile file,
                           @AuthenticationPrincipal UserPrincipal principal,
                           RedirectAttributes redirectAttributes) {
        try {
            modelProfileService.addPhoto(principal.getUser(), file);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/model/profile";
    }

    @PostMapping("/photos/delete")
    public String deletePhoto(@RequestParam("photoUrl") String photoUrl,
                              @AuthenticationPrincipal UserPrincipal principal) {
        modelProfileService.removePhoto(principal.getUser(), photoUrl);
        return "redirect:/model/profile";
    }

    private ModelProfileRequest requestFrom(ModelProfile profile) {
        ModelProfileRequest request = new ModelProfileRequest();
        request.setBio(profile.getBio());
        request.setContactInfo(profile.getContactInfo());
        request.setSocialMediaLinks(profile.getSocialMediaLinks());
        request.setCity(profile.getCity());
        return request;
    }
}
