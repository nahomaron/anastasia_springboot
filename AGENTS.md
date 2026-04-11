# Repository Guidelines

## Project Structure & Module Organization
Source lives under `src/main/java/com/anastasia/Anastasia_BackEnd`, grouped by concern (`controller`, `service`, `repository`, `security`, `mappers`, etc.). Shared assets such as email templates and configuration files are under `src/main/resources`. Tests mirror the main packages in `src/test/java/com/anastasia` with fixtures in `src/test/resources`. High-level docs sit in `docs/`, while UX and architecture sketches are stored in `design/`. Generated Allure reports land in `allure-results/` and should not be committed.

## Build, Test, and Development Commands
- `./mvnw clean verify` — compile, run unit/integration tests, and produce the JaCoCo report in `target/site/jacoco/`.
- `./mvnw spring-boot:run` — start the API with the default profile.
- `./mvnw spring-boot:run -Dspring-boot.run.profiles=api-tests` — boot with the API test profile to load `application-api-tests.yml` overrides.
- `docker compose -f compose.yaml up -d` — launch ancillary services (e.g., database, queues) expected by local development.

## Coding Style & Naming Conventions
Use Java 23 with standard Spring Boot formatting (4-space indentation, braces on same line). Service beans end with `Service`, controllers with `Controller`, repositories extend Spring Data interfaces and use plural domain names. Prefer Lombok for boilerplate (`@Getter`, `@Builder`), MapStruct for DTO mapping, and keep DTOs in `mappers` and `model` packages.

## Testing Guidelines
Write JUnit 5 tests alongside code in matching package paths. Name classes `*Tests` for Spring integration or `*Test` for pure unit tests. Use Mockito for mocking and REST Assured for API verification. Generate Allure results via `./mvnw test` and review HTML reports with `allure serve allure-results`. Maintain ≥80% line coverage; consult JaCoCo output before submitting PRs.

## Commit & Pull Request Guidelines
Follow the existing convention `NN: concise summary`, where `NN` maps to the relevant task or issue number. Each commit should isolate a logical change and include updated documentation or tests. PRs must describe the feature, reference tickets, list breaking changes, and attach screenshots or API samples when behavior shifts. Confirm CI passes and Allure/JaCoCo artifacts are attached when QA review is required.

## Security & Configuration Tips
Never commit secrets; use environment variables or `application-*.yml`. AWS S3 and STS credentials are resolved via IAM roles or local `.env` files ignored by Git. When updating security modules, double-check filters in `security/` and JWT handling classes for regression risks.
