package com.contrapposto.app.controller;

import com.contrapposto.app.dto.ApplicationRequest;
import com.contrapposto.app.security.UserPrincipal;
import com.contrapposto.app.service.EventApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ModelApplicationController {

    private final EventApplicationService eventApplicationService;

    public ModelApplicationController(EventApplicationService eventApplicationService) {
        this.eventApplicationService = eventApplicationService;
    }

    @GetMapping("/model/applications")
    public String list(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        model.addAttribute("applications", eventApplicationService.findForModel(principal.getUser()));
        return "model/applications";
    }

    @PostMapping("/model/events/{eventId}/apply")
    public String apply(@PathVariable Long eventId, @Valid @ModelAttribute("applicationRequest") ApplicationRequest request,
                         BindingResult bindingResult, @AuthenticationPrincipal UserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Message must be at most 1000 characters.");
            return "redirect:/events/" + eventId;
        }
        try {
            eventApplicationService.apply(principal.getUser(), eventId, request.getMessage());
            redirectAttributes.addFlashAttribute("success", "Application submitted.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/events/" + eventId;
    }

    @PostMapping("/model/applications/{id}/accept")
    public String accept(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal,
                          RedirectAttributes redirectAttributes) {
        eventApplicationService.accept(principal.getUser(), id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        redirectAttributes.addFlashAttribute("success", "Invitation accepted.");
        return "redirect:/model/applications";
    }

    @PostMapping("/model/applications/{id}/decline")
    public String decline(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal,
                           RedirectAttributes redirectAttributes) {
        eventApplicationService.decline(principal.getUser(), id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        redirectAttributes.addFlashAttribute("success", "Invitation declined.");
        return "redirect:/model/applications";
    }

    @PostMapping("/model/applications/{id}/withdraw")
    public String withdraw(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal,
                            RedirectAttributes redirectAttributes) {
        eventApplicationService.withdraw(principal.getUser(), id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        redirectAttributes.addFlashAttribute("success", "Application withdrawn.");
        return "redirect:/model/applications";
    }
}
