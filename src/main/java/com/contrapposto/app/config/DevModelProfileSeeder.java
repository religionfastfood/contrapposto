package com.contrapposto.app.config;

import com.contrapposto.app.model.AuthProvider;
import com.contrapposto.app.model.ModelProfile;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.SubscriptionStatus;
import com.contrapposto.app.model.User;
import com.contrapposto.app.repository.ModelProfileRepository;
import com.contrapposto.app.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds a handful of dummy model profiles (with boilerplate bios and stock photo URLs, spread
 * across a few cities, one deliberately LAPSED) so the organizer model-search/browse feature has
 * something to show during manual testing. Reuses the same dev-only gate as {@link DevUserSeeder}.
 * Photo URLs point at randomuser.me's portrait placeholder service rather than real uploads --
 * they need network access to render, which is fine for local dev.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.seed-dev-users", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DevModelProfileSeeder implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "modeldemo123";

    private static final List<Spec> SPECS = List.of(
            new Spec("model.ava@contrapposto.local", "Ava Chen",
                    "Experienced figure model specializing in gesture and long-pose work. Comfortable with all class levels.",
                    "Portland", "ava.chen@example.com", "instagram.com/ava.models",
                    List.of(stockPortrait("women", 33), stockPortrait("women", 68)), SubscriptionStatus.ACTIVE),
            new Spec("model.marcus@contrapposto.local", "Marcus Bell",
                    "Dance and movement background; strong in dynamic gesture sessions and short poses.",
                    "Austin", "marcus.bell@example.com", null,
                    List.of(stockPortrait("men", 45)), SubscriptionStatus.ACTIVE),
            new Spec("model.priya@contrapposto.local", "Priya Nair",
                    "Classically trained figure model. Available for portrait and long-pose sessions, studio or outdoor.",
                    "Chicago", "priya.nair@example.com", "priyanair.art",
                    List.of(stockPortrait("women", 12), stockPortrait("women", 56), stockPortrait("women", 71)), SubscriptionStatus.ACTIVE),
            new Spec("model.diego@contrapposto.local", "Diego Alvarez",
                    "New to modeling but quick to learn poses; enthusiastic and reliable.",
                    "Seattle", "diego.alvarez@example.com", null,
                    List.of(stockPortrait("men", 22)), SubscriptionStatus.ACTIVE),
            new Spec("model.sasha@contrapposto.local", "Sasha Morgan",
                    "Ten years of life-drawing experience. Open studio regular, also books private sessions.",
                    "Portland", "sasha.morgan@example.com", "instagram.com/sasha.draws",
                    List.of(stockPortrait("women", 5), stockPortrait("men", 8)), SubscriptionStatus.ACTIVE),
            new Spec("model.lapsed@contrapposto.local", "Robin Lapsed",
                    "Demo account for a lapsed subscription -- should stay hidden from organizer search.",
                    "Portland", "robin.lapsed@example.com", null,
                    List.of(stockPortrait("men", 71)), SubscriptionStatus.LAPSED)
    );

    private final UserRepository userRepository;
    private final ModelProfileRepository modelProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public DevModelProfileSeeder(UserRepository userRepository,
                                  ModelProfileRepository modelProfileRepository,
                                  PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.modelProfileRepository = modelProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        for (Spec spec : SPECS) {
            if (userRepository.existsByEmail(spec.email())) {
                continue;
            }
            User user = userRepository.save(User.builder()
                    .email(spec.email())
                    .password(passwordEncoder.encode(DEMO_PASSWORD))
                    .role(Role.MODEL)
                    .provider(AuthProvider.LOCAL)
                    .enabled(true)
                    .subscriptionStatus(spec.subscriptionStatus())
                    .build());

            ModelProfile profile = new ModelProfile(user);
            profile.setDisplayName(spec.displayName());
            profile.setBio(spec.bio());
            profile.setCity(spec.city());
            profile.setContactInfo(spec.contactInfo());
            profile.setSocialMediaLinks(spec.socialMediaLinks());
            profile.getPhotoUrls().addAll(spec.photoUrls());
            modelProfileRepository.save(profile);

            log.info("Seeded dev model profile: {} ({})", spec.displayName(), spec.email());
        }
    }

    private static String stockPortrait(String gender, int index) {
        return "https://randomuser.me/api/portraits/" + gender + "/" + index + ".jpg";
    }

    private record Spec(String email, String displayName, String bio, String city, String contactInfo,
                         String socialMediaLinks, List<String> photoUrls, SubscriptionStatus subscriptionStatus) {
    }
}
