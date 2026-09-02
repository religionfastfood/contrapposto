package com.contrapposto.app.security;

import com.contrapposto.app.model.AuthProvider;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.User;
import com.contrapposto.app.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;

    public OAuth2AuthenticationSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String providerId = oAuth2User.getName();

        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            String targetUrl = switch (user.getRole()) {
                case ADMIN -> "/admin/dashboard";
                case ORGANIZER -> "/organizer/dashboard";
                default -> "/model/dashboard";
            };
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            return;
        }

        // New user — check for pending role in session
        HttpSession session = request.getSession(false);
        Role pendingRole = (session != null) ? (Role) session.getAttribute("pendingRole") : null;

        if (pendingRole == null) {
            getRedirectStrategy().sendRedirect(request, response, "/register?oauth=pending");
            return;
        }

        User newUser = User.builder()
                .email(email)
                .provider(AuthProvider.GOOGLE)
                .providerId(providerId)
                .role(pendingRole)
                .enabled(true)
                .build();
        userRepository.save(newUser);

        session.removeAttribute("pendingRole");

        String targetUrl = switch (pendingRole) {
            case ADMIN -> "/admin/dashboard";
            case ORGANIZER -> "/organizer/dashboard";
            default -> "/model/dashboard";
        };
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}