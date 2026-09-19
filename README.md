# Contrapposto

A scheduling platform for life drawing events. Connects organizers who run figure drawing sessions with professional models.

## What it does

- **Organizers** post and manage events (title, type, date/time, location, price), browse and search models by city, and manage their organization profile
- **Models** create a profile with up to 3 photos, and are discoverable by organizers once subscribed
- **Admins** manage the shared list of event types (Gesture, Short Pose, Long Pose, Portrait, Open Studio, etc.) that organizers choose from when posting an event
- **Anonymous visitors** can browse upcoming events by city and view public event detail pages
- Models and organizers each require an active subscription (Stripe-backed, with a free trial) to use the platform; a lapsed organizer can still view past events but can't post new ones, and a lapsed model drops out of search

## Tech Stack

- Java 21 / Spring Boot 4.0.4
- Spring MVC + Thymeleaf + HTMX (server-driven UI, no SPA)
- Spring Security 7 — form login + optional Google OAuth2
- Spring Data JPA + HSQLDB (in-memory, dev only)
- Stripe (subscriptions) — the app runs fine unconfigured; subscription features just no-op
- AWS S3 (model photo storage) — optional; falls back to local disk storage (`./local-uploads/`) when unconfigured, so photo upload works out of the box in dev
- jqwik (property-based testing) alongside JUnit 5, plus JaCoCo for coverage reporting

## Getting Started

**Prerequisites:** Java 21, Maven (or use the included `./mvnw` wrapper)

```bash
# Run the app
./mvnw spring-boot:run
```

Visit [http://localhost:8080](http://localhost:8080).

### Try it out

On startup, the app seeds a handful of accounts and sample data so you can explore without registering:

| Role | Email | Password |
|---|---|---|
| Admin | `admin@contrapposto.local` | `admin123` |
| Organizer | `organizer@contrapposto.local` | `organizer123` |
| Model | `model@contrapposto.local` | `model123` |

It also seeds 6 sample model profiles (`model.ava@contrapposto.local`, `model.marcus@contrapposto.local`, etc. — password `modeldemo123` for all) with photos, bios, and varied cities, so the organizer-facing model search has something to show. One of them has a lapsed subscription on purpose, to demonstrate that lapsed models are hidden from search.

This seeding is on by default and meant for local development — disable it with `SEED_DEV_USERS=false` before any real deployment.

### Optional integrations

None of these are required to run the app locally — each feature degrades gracefully when unconfigured.

**Stripe (subscriptions):**
```
STRIPE_SECRET_KEY=...
STRIPE_WEBHOOK_SECRET=...
STRIPE_PRICE_MODEL_MONTHLY=...
STRIPE_PRICE_MODEL_ANNUAL=...
STRIPE_PRICE_ORGANIZER_MONTHLY=...
STRIPE_PRICE_ORGANIZER_ANNUAL=...
```

**AWS S3 (model photo storage — omit to use local disk instead):**
```
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...
AWS_REGION=...
AWS_S3_BUCKET_NAME=...
```

**Google OAuth2:** set the env vars below, then uncomment the three corresponding lines in `src/main/resources/application.properties`:
```
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret
```

## Development

```bash
# Build
./mvnw clean package

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=UserServiceImplTest
```

Each phase of work is developed on its own `phase-N` branch and merged to `master` once complete — see `CLAUDE.md` for the full branching and testing policy.

## Project Status

| Phase | Status |
|---|---|
| Phase 1 — Foundation (auth, registration, dashboards) | Complete |
| Phase 2 — Stripe subscriptions | Complete |
| Phase 3 — Profiles & photos | Complete |
| Phase 4 — Model search & public profiles | Complete |
| Phase 5 — Events | Complete |
| Phase 6 — Applications & invitations | Planned |
