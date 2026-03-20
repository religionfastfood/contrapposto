package com.contrapposto.app.security;

import com.contrapposto.app.model.Role;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class FormLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Role role = principal.getUser().getRole();

        String targetUrl;
        if (role == Role.ORGANIZER) {
            targetUrl = "/organizer/dashboard";
        } else {
            targetUrl = "/model/dashboard";
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}