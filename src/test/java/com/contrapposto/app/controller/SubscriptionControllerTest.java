package com.contrapposto.app.controller;

import com.contrapposto.app.config.SecurityConfig;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.SubscriptionStatus;
import com.contrapposto.app.model.User;
import com.contrapposto.app.security.CustomUserDetailsService;
import com.contrapposto.app.security.FormLoginSuccessHandler;
import com.contrapposto.app.security.OAuth2AuthenticationSuccessHandler;
import com.contrapposto.app.security.UserPrincipal;
import com.contrapposto.app.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SubscriptionController.class)
@Import(SecurityConfig.class)
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private FormLoginSuccessHandler formLoginSuccessHandler;

    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    // --- /subscription/plan ---

    @Test
    void getPlan_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/subscription/plan"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void getPlan_authenticated_returnsOk() throws Exception {
        when(subscriptionService.isConfigured()).thenReturn(false);

        mockMvc.perform(get("/subscription/plan").with(user(modelPrincipal())))
                .andExpect(status().isOk())
                .andExpect(view().name("subscription/plan"))
                .andExpect(model().attributeExists("role", "monthlyPrice", "annualPrice"));
    }

    @Test
    void getPlan_forModel_showsModelPricing() throws Exception {
        when(subscriptionService.isConfigured()).thenReturn(false);

        mockMvc.perform(get("/subscription/plan").with(user(modelPrincipal())))
                .andExpect(model().attribute("monthlyPrice", "$5"))
                .andExpect(model().attribute("annualPrice", "$48"));
    }

    @Test
    void getPlan_forOrganizer_showsOrganizerPricing() throws Exception {
        when(subscriptionService.isConfigured()).thenReturn(false);

        mockMvc.perform(get("/subscription/plan").with(user(organizerPrincipal())))
                .andExpect(model().attribute("monthlyPrice", "$10"))
                .andExpect(model().attribute("annualPrice", "$96"));
    }

    // --- /subscription/checkout ---

    @Test
    void postCheckout_stripeNotConfigured_redirectsWithError() throws Exception {
        when(subscriptionService.isConfigured()).thenReturn(false);

        mockMvc.perform(post("/subscription/checkout").with(csrf()).with(user(modelPrincipal()))
                        .param("billingPeriod", "MONTHLY"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/subscription/plan"));
    }

    @Test
    void postCheckout_stripeConfigured_redirectsToStripe() throws Exception {
        when(subscriptionService.isConfigured()).thenReturn(true);
        when(subscriptionService.createCheckoutSession(any(), any(), any(), any()))
                .thenReturn("https://checkout.stripe.com/test-session");

        mockMvc.perform(post("/subscription/checkout").with(csrf()).with(user(modelPrincipal()))
                        .param("billingPeriod", "MONTHLY"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://checkout.stripe.com/test-session"));
    }

    @Test
    void postCheckout_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/subscription/checkout").with(csrf())
                        .param("billingPeriod", "MONTHLY"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // --- /subscription/success and /cancel ---

    @Test
    @WithMockUser
    void getSuccess_returnsOk() throws Exception {
        mockMvc.perform(get("/subscription/success"))
                .andExpect(status().isOk())
                .andExpect(view().name("subscription/success"));
    }

    @Test
    @WithMockUser
    void getCancel_returnsOk() throws Exception {
        mockMvc.perform(get("/subscription/cancel"))
                .andExpect(status().isOk())
                .andExpect(view().name("subscription/cancel"));
    }

    // --- Helpers ---

    private UserPrincipal modelPrincipal() {
        User user = User.builder()
                .id(1L)
                .email("model@example.com")
                .role(Role.MODEL)
                .subscriptionStatus(SubscriptionStatus.NONE)
                .enabled(true)
                .build();
        return new UserPrincipal(user);
    }

    private UserPrincipal organizerPrincipal() {
        User user = User.builder()
                .id(2L)
                .email("organizer@example.com")
                .role(Role.ORGANIZER)
                .subscriptionStatus(SubscriptionStatus.NONE)
                .enabled(true)
                .build();
        return new UserPrincipal(user);
    }
}
