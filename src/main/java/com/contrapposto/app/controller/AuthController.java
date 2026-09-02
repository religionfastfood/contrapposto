package com.contrapposto.app.controller;

import com.contrapposto.app.dto.RegisterRequest;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String registered,
                        Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email or password.");
        }
        if (registered != null) {
            model.addAttribute("successMessage", "Account created! Please log in.");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        model.addAttribute("roles", Role.values());
        return "auth/register";
    }

    @GetMapping("/register/form")
    public String registerForm(@RequestParam Role role, Model model) {
        model.addAttribute("role", role);
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register-form-fragment :: registerForm";
    }

    @PostMapping("/register")
    public String registerSubmit(@Valid @ModelAttribute RegisterRequest registerRequest,
                                 BindingResult bindingResult,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("role", registerRequest.getRole());
            return "auth/register-form-fragment";
        }

        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            model.addAttribute("error", "Passwords do not match.");
            model.addAttribute("role", registerRequest.getRole());
            return "auth/register-form-fragment";
        }

        try {
            userService.register(registerRequest);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("role", registerRequest.getRole());
            return "auth/register-form-fragment";
        }

        return "redirect:/login?registered=true";
    }

    @GetMapping("/register/oauth2/google")
    public String registerOAuth2Google(@RequestParam Role role, HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        session.setAttribute("pendingRole", role);
        return "redirect:/oauth2/authorization/google";
    }
}