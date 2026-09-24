# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build
./mvnw clean package

# Run the application (requires Java 21)
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=ContrappostoApplicationTests

# Run a single test method
./mvnw test -Dtest=ContrappostoApplicationTests#contextLoads
```

## Tech Stack

- **Java 21** / **Spring Boot 4.0.4**
- **Spring MVC** with **Thymeleaf** + **HTMX** (`htmx-spring-boot-thymeleaf 5.0.0`) for server-driven interactivity
- **Spring Security 7** with form login + conditional Google OAuth2; Thymeleaf Security extras for template-level auth checks
- **Spring Data JPA** with **HSQLDB** (in-memory, dev only)
- **Spring AI 2.0.0-M3** with Anthropic model starter
- **Lombok**
- **Bootstrap 5.3** + **HTMX 2.0** via CDN; navy/cream color theme

## Environment Variables

| Variable | Purpose |
|---|---|
| `GOOGLE_CLIENT_ID` | Google OAuth2 (optional — app runs without it) |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 (optional) |

Google OAuth2 is disabled by default. To enable, uncomment the three lines in `application.properties` and set the env vars.

## Architecture

Server-side-rendered app. Thymeleaf renders full pages; HTMX handles partial updates (e.g. the registration role-selection flow loads the signup form inline without a page reload).

**Package structure** (`com.contrapposto.app`):
- `model/` — JPA entities and enums (`User`, `Role`, `AuthProvider`)
- `repository/` — Spring Data JPA repositories
- `dto/` — Form-binding objects (`RegisterRequest`)
- `event/` — Domain events published via `ApplicationEventPublisher` (e.g. `ApplicationSubmittedEvent`); consumed by `@Async @TransactionalEventListener(AFTER_COMMIT)` listeners in `service/` so side effects like notifications never fire for a transaction that rolls back
- `service/` — Business logic interfaces + implementations
- `security/` — `UserPrincipal`, `CustomUserDetailsService`, success handlers
- `config/` — `SecurityConfig`
- `controller/` — MVC controllers
- `src/main/resources/templates/` — Thymeleaf templates; shared fragments in `fragments/layout.html`

**Auth flow:**
- Form login uses `CustomUserDetailsService` → `UserPrincipal` → `FormLoginSuccessHandler` (redirects by role)
- Google OAuth2 (when enabled): user selects role first → role stored in HTTP session → `OAuth2AuthenticationSuccessHandler` creates account on callback
- Routes: `/organizer/**` requires `ROLE_ORGANIZER`, `/model/**` requires `ROLE_MODEL`

## App Description

Contrapposto is a scheduling platform for **Life Drawing events** (artists gather to draw a human model pose). Three user types: Organizer, Model, Anonymous.

**Subscription model** (Stripe, Phase 2):
- Free for anonymous users
- Models: $5/mo or $48/yr; Organizers: $10/mo or $96/yr
- First month free trial, payment method required upfront
- Lapsed organizer: can view past events, cannot post new ones
- Lapsed model: hidden from search, but still shown on assigned events

## Branching Policy

Each new phase must be developed on a dedicated feature branch named `phase-N` (e.g. `phase-2`). Create the branch before writing any phase code. Merge back into `master` only when all tests pass and the phase is complete. Never commit phase work directly to `master`.

## Testing Policy

Always write unit tests alongside new code. All tests must pass before any code is pushed — run `./mvnw test` and confirm `BUILD SUCCESS` before pushing. Every new service class gets a `*Test` in `src/test/.../service/`. Every new controller gets a `*Test` in `src/test/.../controller/` using `@WebMvcTest` + `@Import(SecurityConfig.class)`.

**Property-based testing (Phase 2 onward):** Use [jqwik](https://jqwik.net/) for service-layer logic with non-trivial invariants — subscription state rules, pricing calculations, date/availability logic. Every service test class from Phase 2 onward should include at least one `@Property` alongside its example-based `@Test` methods where a meaningful property exists. Annotate PBT test classes with `@ExtendWith(JqwikSpringExtension.class)` if Spring context is needed, otherwise plain `@Property` methods work without any extra annotation.

**Conventions learned from Spring Boot 4:**
- Use `@MockitoBean` (not `@MockBean`) — package: `org.springframework.test.context.bean.override.mockito`
- Use `@WebMvcTest` from `org.springframework.boot.webmvc.test.autoconfigure`
- Always `@Import(SecurityConfig.class)` in controller tests — it is not auto-detected in the test slice
- Mock `CustomUserDetailsService`, `FormLoginSuccessHandler`, and `OAuth2AuthenticationSuccessHandler` in every controller test (required by `SecurityConfig` constructor)
- Use `.with(csrf())` on all POST requests in controller tests

## Build Phases

- **Phase 1 — Foundation** ✅ COMPLETE — User/auth/registration/dashboards
- **Phase 2 — Stripe Subscriptions** — Checkout, webhooks, subscription enforcement
- **Phase 3 — Profiles & Photos** — ModelProfile, OrganizerProfile, AWS S3 photo upload
- **Phase 4 — Model Search & Public Profiles** — Public read-only model profile view, organizer-facing model search/browse by city, lapsed-model hiding per subscription rules
- **Phase 5 — Events** — Event CRUD, EventType, public listings by city, event detail page
- **Phase 6 — Applications & Invitations** — Apply/invite flows, approve/decline, email notifications

**Future:** In-app event ticketing via Stripe (design Events with this in mind — store price as amount+currency, keep ticketing as separate entities).