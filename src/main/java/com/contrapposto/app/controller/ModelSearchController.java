package com.contrapposto.app.controller;

import com.contrapposto.app.model.ModelProfile;
import com.contrapposto.app.service.ModelSearchService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/organizer/models")
public class ModelSearchController {

    private final ModelSearchService modelSearchService;

    public ModelSearchController(ModelSearchService modelSearchService) {
        this.modelSearchService = modelSearchService;
    }

    @GetMapping
    public String search(@RequestParam(required = false) String city, Model model) {
        model.addAttribute("profiles", modelSearchService.search(city));
        model.addAttribute("city", city);
        return "organizer/models";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        ModelProfile profile = modelSearchService.findVisibleProfile(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Model not found"));
        model.addAttribute("profile", profile);
        return "organizer/model-detail";
    }
}
