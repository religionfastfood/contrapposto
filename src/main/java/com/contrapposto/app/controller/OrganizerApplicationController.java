package com.contrapposto.app.controller;

import com.contrapposto.app.dto.InviteRequest;
import com.contrapposto.app.model.ApplicationInitiator;
import com.contrapposto.app.model.ApplicationStatus;
import com.contrapposto.app.model.EventApplication;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class OrganizerApplicationController {

    private final EventApplicationService eventApplicationService;

    public OrganizerApplicationController(EventApplicationService eventApplicationService) {
        this.eventApplicationService = eventApplicationService;
    }

    @GetMapping("/organizer/applications")
    public String list(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        List<EventApplication> applications = eventApplicationService.findForOrganizer(principal.getUser());
        model.addAttribute("applications", applications);
        model.addAttribute("priorDeclines", priorDeclinesByApplicationId(applications));
        return "organizer/applications";
    }

    // Flags PENDING model-initiated applications where this model was previously declined for the
    // same event -- a hint for the organizer's review, not an enforced block (see EventApplicationService).
    private Map<Long, Boolean> priorDeclinesByApplicationId(List<EventApplication> applications) {
        Map<Long, Boolean> result = new HashMap<>();
        for (EventApplication application : applications) {
            if (application.getInitiatedBy() == ApplicationInitiator.MODEL && application.getStatus() == ApplicationStatus.PENDING) {
                result.put(application.getId(), eventApplicationService.hasPriorDecline(application.getEvent(), application.getModel()));
            }
        }
        return result;
    }

    @PostMapping("/organizer/models/{modelId}/invite")
    public String invite(@PathVariable Long modelId, @Valid @ModelAttribute("inviteRequest") InviteRequest request,
                          BindingResult bindingResult, @AuthenticationPrincipal UserPrincipal principal,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please choose an event and keep the message under 1000 characters.");
            return "redirect:/organizer/models/" + modelId;
        }
        try {
            eventApplicationService.invite(principal.getUser(), request.getEventId(), modelId, request.getMessage());
            redirectAttributes.addFlashAttribute("success", "Invitation sent.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/organizer/models/" + modelId;
    }

    @PostMapping("/organizer/applications/{id}/accept")
    public String accept(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal,
                          RedirectAttributes redirectAttributes) {
        eventApplicationService.accept(principal.getUser(), id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        redirectAttributes.addFlashAttribute("success", "Application accepted.");
        return "redirect:/organizer/applications";
    }

    @PostMapping("/organizer/applications/{id}/decline")
    public String decline(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal,
                           RedirectAttributes redirectAttributes) {
        eventApplicationService.decline(principal.getUser(), id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        redirectAttributes.addFlashAttribute("success", "Application declined.");
        return "redirect:/organizer/applications";
    }

    @PostMapping("/organizer/applications/{id}/withdraw")
    public String withdraw(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal,
                            RedirectAttributes redirectAttributes) {
        eventApplicationService.withdraw(principal.getUser(), id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        redirectAttributes.addFlashAttribute("success", "Invitation withdrawn.");
        return "redirect:/organizer/applications";
    }
}
