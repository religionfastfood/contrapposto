package com.contrapposto.app.controller;

import com.contrapposto.app.model.BillingPeriod;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.User;
import com.contrapposto.app.security.UserPrincipal;
import com.contrapposto.app.service.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/subscription")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/plan")
    public String plan(Model model, @AuthenticationPrincipal UserPrincipal principal) {
        User user = principal.getUser();
        Role role = user.getRole();

        model.addAttribute("role", role);
        model.addAttribute("stripeConfigured", subscriptionService.isConfigured());

        if (role == Role.MODEL) {
            model.addAttribute("monthlyPrice", "$5");
            model.addAttribute("monthlyUnit", "per month");
            model.addAttribute("annualPrice", "$48");
            model.addAttribute("annualUnit", "per year ($4/mo)");
        } else {
            model.addAttribute("monthlyPrice", "$10");
            model.addAttribute("monthlyUnit", "per month");
            model.addAttribute("annualPrice", "$96");
            model.addAttribute("annualUnit", "per year ($8/mo)");
        }

        return "subscription/plan";
    }

    @PostMapping("/checkout")
    public String checkout(@RequestParam BillingPeriod billingPeriod,
                           @AuthenticationPrincipal UserPrincipal principal,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {
        if (!subscriptionService.isConfigured()) {
            redirectAttributes.addFlashAttribute("error",
                    "Payment processing is not yet available. Please check back soon.");
            return "redirect:/subscription/plan";
        }

        User user = principal.getUser();
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        String successUrl = baseUrl + "/subscription/success?session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = baseUrl + "/subscription/cancel";

        try {
            String checkoutUrl = subscriptionService.createCheckoutSession(user, billingPeriod, successUrl, cancelUrl);
            return "redirect:" + checkoutUrl;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Something went wrong starting your subscription. Please try again.");
            return "redirect:/subscription/plan";
        }
    }

    @GetMapping("/success")
    public String success(Model model) {
        model.addAttribute("message", "Your subscription is active. Welcome to Contrapposto!");
        return "subscription/success";
    }

    @GetMapping("/cancel")
    public String cancel() {
        return "subscription/cancel";
    }
}
