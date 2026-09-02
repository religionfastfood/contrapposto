package com.contrapposto.app.controller;

import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.User;
import com.contrapposto.app.security.CustomUserDetailsService;
import com.contrapposto.app.security.FormLoginSuccessHandler;
import com.contrapposto.app.security.OAuth2AuthenticationSuccessHandler;
import com.contrapposto.app.service.UserService;
import com.contrapposto.app.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private FormLoginSuccessHandler formLoginSuccessHandler;

    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Test
    void getRegister_returnsOkWithRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registerRequest"))
                .andExpect(model().attributeExists("roles"));
    }

    @Test
    void getRegisterForm_withModelRole_returnsFragment() throws Exception {
        mockMvc.perform(get("/register/form").param("role", "MODEL"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register-form-fragment :: registerForm"))
                .andExpect(model().attribute("role", Role.MODEL));
    }

    @Test
    void getRegisterForm_withOrganizerRole_returnsFragment() throws Exception {
        mockMvc.perform(get("/register/form").param("role", "ORGANIZER"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register-form-fragment :: registerForm"))
                .andExpect(model().attribute("role", Role.ORGANIZER));
    }

    @Test
    void postRegister_withValidData_redirectsToLogin() throws Exception {
        when(userService.register(any())).thenReturn(new User());

        mockMvc.perform(post("/register").with(csrf())
                        .param("email", "test@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123")
                        .param("role", "MODEL"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));
    }

    @Test
    void postRegister_withMismatchedPasswords_returnsFormWithError() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("email", "test@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "different")
                        .param("role", "MODEL"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register-form-fragment"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void postRegister_withDuplicateEmail_returnsFormWithError() throws Exception {
        when(userService.register(any()))
                .thenThrow(new IllegalArgumentException("An account with that email already exists"));

        mockMvc.perform(post("/register").with(csrf())
                        .param("email", "existing@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123")
                        .param("role", "MODEL"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register-form-fragment"))
                .andExpect(model().attribute("error", "An account with that email already exists"));
    }

    @Test
    void postRegister_withBlankEmail_returnsFormWithFieldError() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("email", "")
                        .param("password", "password123")
                        .param("confirmPassword", "password123")
                        .param("role", "MODEL"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register-form-fragment"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "email"));
    }

    @Test
    void postRegister_withInvalidEmailFormat_returnsFormWithFieldError() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("email", "notanemail")
                        .param("password", "password123")
                        .param("confirmPassword", "password123")
                        .param("role", "MODEL"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register-form-fragment"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "email"));
    }

    @Test
    void postRegister_withShortPassword_returnsFormWithFieldError() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("email", "test@example.com")
                        .param("password", "short")
                        .param("confirmPassword", "short")
                        .param("role", "MODEL"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register-form-fragment"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "password"));
    }

    @Test
    void postRegister_withBlankPassword_returnsFormWithFieldError() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("email", "test@example.com")
                        .param("password", "")
                        .param("confirmPassword", "")
                        .param("role", "MODEL"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register-form-fragment"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "password"));
    }

    @Test
    void getLogin_returnsOkWithLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void getLogin_withErrorParam_addsErrorMessage() throws Exception {
        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    void getLogin_withRegisteredParam_addsSuccessMessage() throws Exception {
        mockMvc.perform(get("/login").param("registered", ""))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("successMessage"));
    }
}