package com.contrapposto.app.config;

import com.contrapposto.app.model.AuthProvider;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.SubscriptionStatus;
import com.contrapposto.app.model.User;
import com.contrapposto.app.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.seed-dev-users", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DevUserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final List<Spec> specs;

    public DevUserSeeder(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.seed-dev-users.admin-email}") String adminEmail,
                          @Value("${app.seed-dev-users.admin-password}") String adminPassword,
                          @Value("${app.seed-dev-users.model-email}") String modelEmail,
                          @Value("${app.seed-dev-users.model-password}") String modelPassword,
                          @Value("${app.seed-dev-users.organizer-email}") String organizerEmail,
                          @Value("${app.seed-dev-users.organizer-password}") String organizerPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.specs = List.of(
                new Spec(adminEmail, adminPassword, Role.ADMIN, SubscriptionStatus.NONE),
                new Spec(modelEmail, modelPassword, Role.MODEL, SubscriptionStatus.ACTIVE),
                new Spec(organizerEmail, organizerPassword, Role.ORGANIZER, SubscriptionStatus.ACTIVE)
        );
    }

    @Override
    public void run(String... args) {
        for (Spec spec : specs) {
            if (userRepository.existsByEmail(spec.email())) {
                continue;
            }
            User user = User.builder()
                    .email(spec.email())
                    .password(passwordEncoder.encode(spec.password()))
                    .role(spec.role())
                    .provider(AuthProvider.LOCAL)
                    .enabled(true)
                    .subscriptionStatus(spec.subscriptionStatus())
                    .build();
            userRepository.save(user);
            log.info("Seeded dev {} user: {}", spec.role(), spec.email());
        }
    }

    private record Spec(String email, String password, Role role, SubscriptionStatus subscriptionStatus) {
    }
}
