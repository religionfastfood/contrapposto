package com.contrapposto.app.controller;

import com.contrapposto.app.config.SecurityConfig;
import com.contrapposto.app.model.ModelProfile;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.User;
import com.contrapposto.app.security.CustomUserDetailsService;
import com.contrapposto.app.security.FormLoginSuccessHandler;
import com.contrapposto.app.security.OAuth2AuthenticationSuccessHandler;
import com.contrapposto.app.security.UserPrincipal;
import com.contrapposto.app.service.ModelProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ModelProfileController.class)
@Import(SecurityConfig.class)
class ModelProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModelProfileService modelProfileService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private FormLoginSuccessHandler formLoginSuccessHandler;

    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Test
    void getProfile_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/model/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void getProfile_wrongRole_isForbidden() throws Exception {
        mockMvc.perform(get("/model/profile").with(user(organizerPrincipal())))
                .andExpect(status().isForbidden());
    }

    @Test
    void getProfile_asModel_returnsOk() throws Exception {
        User user = modelUser();
        when(modelProfileService.getOrCreateProfile(any())).thenReturn(new ModelProfile(user));

        mockMvc.perform(get("/model/profile").with(user(new UserPrincipal(user))))
                .andExpect(status().isOk())
                .andExpect(view().name("model/profile"))
                .andExpect(model().attributeExists("profile", "profileRequest"));
    }

    @Test
    void postProfile_validData_updatesAndReturnsOk() throws Exception {
        User user = modelUser();
        when(modelProfileService.updateProfile(any(), any())).thenReturn(new ModelProfile(user));
        when(modelProfileService.getOrCreateProfile(any())).thenReturn(new ModelProfile(user));

        mockMvc.perform(post("/model/profile").with(csrf()).with(user(new UserPrincipal(user)))
                        .param("bio", "A bio")
                        .param("city", "Portland"))
                .andExpect(status().isOk())
                .andExpect(view().name("model/profile"))
                .andExpect(model().attributeExists("success"));
    }

    @Test
    void postProfile_bioTooLong_returnsFormWithFieldError() throws Exception {
        User user = modelUser();
        when(modelProfileService.getOrCreateProfile(any())).thenReturn(new ModelProfile(user));

        mockMvc.perform(post("/model/profile").with(csrf()).with(user(new UserPrincipal(user)))
                        .param("bio", "x".repeat(2001)))
                .andExpect(status().isOk())
                .andExpect(view().name("model/profile"))
                .andExpect(model().attributeHasFieldErrors("profileRequest", "bio"));
    }

    @Test
    void postPhoto_success_redirectsToProfile() throws Exception {
        User user = modelUser();
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", "bytes".getBytes());
        when(modelProfileService.addPhoto(any(), any())).thenReturn(new ModelProfile(user));

        mockMvc.perform(multipart("/model/profile/photos").file(file)
                        .with(csrf()).with(user(new UserPrincipal(user))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/model/profile"));
    }

    @Test
    void postPhoto_atCap_redirectsWithFlashError() throws Exception {
        User user = modelUser();
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", "bytes".getBytes());
        when(modelProfileService.addPhoto(any(), any()))
                .thenThrow(new IllegalArgumentException("You can only have up to 3 photos"));

        mockMvc.perform(multipart("/model/profile/photos").file(file)
                        .with(csrf()).with(user(new UserPrincipal(user))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/model/profile"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void postDeletePhoto_redirectsToProfile() throws Exception {
        User user = modelUser();
        when(modelProfileService.removePhoto(any(), eq("/uploads/a.jpg"))).thenReturn(new ModelProfile(user));

        mockMvc.perform(post("/model/profile/photos/delete").with(csrf()).with(user(new UserPrincipal(user)))
                        .param("photoUrl", "/uploads/a.jpg"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/model/profile"));
    }

    // --- Helpers ---

    private User modelUser() {
        return User.builder().id(1L).email("model@example.com").role(Role.MODEL).enabled(true).build();
    }

    private UserPrincipal organizerPrincipal() {
        User user = User.builder().id(2L).email("organizer@example.com").role(Role.ORGANIZER).enabled(true).build();
        return new UserPrincipal(user);
    }
}
