# Contrapposto

A scheduling platform for life drawing events. Connects organizers who run figure drawing sessions with professional models.

## What it does

- **Organizers** post events, browse models, and manage bookings
- **Models** create profiles, browse events by city, and apply to sessions
- **Anonymous users** can discover events and view public event details

## Tech Stack

- Java 21 / Spring Boot 4.0.4
- Spring MVC + Thymeleaf + HTMX (server-driven UI, no SPA)
- Spring Security 7 — form login + optional Google OAuth2
- Spring Data JPA + HSQLDB (in-memory, dev)
- Bootstrap 5.3 (navy/cream theme)

## Getting Started

**Prerequisites:** Java 21, Maven (or use the included `./mvnw` wrapper)

```bash
# Run the app
./mvnw spring-boot:run
```

Visit [http://localhost:8080](http://localhost:8080).

### Google OAuth2 (optional)

Set the following environment variables, then uncomment the three lines in `src/main/resources/application.properties`:

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

## Project Status

| Phase | Status |
|---|---|
| Phase 1 — User auth & registration | Complete |
| Phase 2 — Stripe subscriptions | Planned |
| Phase 3 — Profiles & photos (S3) | Planned |
| Phase 4 — Events | Planned |
| Phase 5 — Applications & invitations | Planned |
