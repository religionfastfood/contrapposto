package com.contrapposto.app.config;

import com.contrapposto.app.security.CustomUserDetailsService;
import com.contrapposto.app.security.FormLoginSuccessHandler;
import com.contrapposto.app.security.OAuth2AuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    private final FormLoginSuccessHandler formLoginSuccessHandler;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
                          OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler,
                          FormLoginSuccessHandler formLoginSuccessHandler) {
        this.customUserDetailsService = customUserDetailsService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.formLoginSuccessHandler = formLoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/events/**", "/login", "/register/**",
                        "/css/**", "/js/**", "/webjars/**", "/oauth2/**").permitAll()
                .requestMatchers("/organizer/**").hasRole("ORGANIZER")
                .requestMatchers("/model/**").hasRole("MODEL")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(formLoginSuccessHandler)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .successHandler(oAuth2SuccessHandler)
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }
}