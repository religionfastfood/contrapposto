# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build
./mvnw clean package

# Run the application
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
- **Spring MVC** with **Thymeleaf** templates + **HTMX** (`htmx-spring-boot-thymeleaf 5.0.0`) for server-driven interactivity
- **Spring Security** with **OAuth2 client** login; Thymeleaf Security extras for template-level auth checks
- **Spring Data JPA** with **HSQLDB** (in-memory) for persistence
- **Spring AI 2.0.0-M3** with the Anthropic model starter (`spring-ai-starter-model-anthropic`)
- **Lombok** for reducing boilerplate

## Architecture Notes

This project is in early scaffolding stage. The intended architecture is a server-side-rendered web app where:

- Thymeleaf renders HTML pages, with HTMX handling partial page updates without a full JS frontend framework
- Spring Security secures routes via OAuth2 (provider config will need to be added to `application.properties`)
- Spring AI integrates Claude (Anthropic) as the AI backend — configure via `spring.ai.anthropic.api-key` in `application.properties` or environment
- JPA entities + repositories will back persistence with HSQLDB in dev (swap to a persistent DB for production)

The base package is `com.contrapposto.app`.