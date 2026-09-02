package com.contrapposto.app.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/organizer/dashboard")
    @PreAuthorize("hasRole('ORGANIZER')")
    public String organizerDashboard() {
        return "organizer/dashboard";
    }

    @GetMapping("/model/dashboard")
    @PreAuthorize("hasRole('MODEL')")
    public String modelDashboard() {
        return "model/dashboard";
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard() {
        return "admin/dashboard";
    }
}