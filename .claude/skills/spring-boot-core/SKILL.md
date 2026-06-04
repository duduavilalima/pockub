---
name: spring-boot-core
description: Use this skill when creating or updating Spring Boot application code in this repository, including REST controllers, services, DTOs, validation, exception handling, configuration, and general project structure.
---

# Spring Boot core rules

## Scope
Apply this skill when working on:
- REST API endpoints
- controllers, services, and repositories
- DTOs and validation
- exception handling
- application configuration
- general Spring Boot project structure

## Architecture
- Use a clear layered architecture: controller -> service -> repository.
- Keep controllers thin and focused on HTTP concerns.
- Put business logic in services.
- Keep repositories focused on persistence and querying.
- Keep package structure consistent and easy to navigate.
- Prefer simple, explicit designs over unnecessary abstraction.

## Build and project conventions
- Use Maven, not Gradle.
- Prefer explicit, readable code over generated magic.
- Keep the project easy to build and run across environments.
- When suggesting project setup improvements, prefer practical repository hygiene such as `.gitignore`, `.env.sample`, `.editorconfig`, `.gitattributes`, and `.dockerignore` when relevant.

## Spring Boot and Java conventions
- Generate idiomatic Spring Boot code aligned with modern Spring practices.
- Prefer constructor injection.
- Use Spring Boot starters and framework conventions instead of custom plumbing when possible.
- Keep configuration externalized and environment-aware.
- Prefer `.properties` files for configuration unless the project clearly uses another format.
- Do not hardcode secrets or environment-specific values in source code.

## REST API rules
- Use resource-oriented endpoint design.
- Use the correct HTTP method for each operation.
- Return meaningful HTTP status codes.
- Keep request and response models stable and explicit.
- Do not expose JPA entities directly from controllers.
- Use DTOs for API boundaries.

## DTO and validation rules
- Use dedicated request and response DTOs.
- Apply Jakarta Validation annotations to incoming request DTOs.
- Keep validation rules close to the request model.
- Return clear validation errors.
- Avoid leaking internal domain or persistence details in API responses.

## Service layer rules
- Put business rules in services.
- Keep methods cohesive and intention-revealing.
- Define transaction boundaries in the service layer where needed.
- Do not move business decisions into controllers.

## Exception handling
- Use custom exceptions for business cases such as not found or invalid state.
- Use a global exception handler for consistent API error responses.
- Prefer clear, client-facing error messages.
- Do not leak stack traces or internal implementation details in API responses.

## Configuration guidance
- Externalize configuration using Spring Boot configuration mechanisms.
- Use environment variables or profile-specific properties for environment-dependent values.
- Never commit real secrets.
- Prefer explicit settings for database, logging, and runtime behavior.
- Keep `spring.jpa.open-in-view=false` unless there is a deliberate reason not to.

## Database awareness
- Default to PostgreSQL-oriented behavior for persistence-related decisions.
- Avoid assumptions that only work in in-memory databases.
- Align entity, validation, and API design with realistic database constraints.
- If schema changes are needed, follow the repository’s schema management approach rather than inventing a second mechanism.

## Logging
- Use SLF4J with the default Spring Boot logging stack.
- Use parameterized logging.
- Do not log secrets, tokens, passwords, or other sensitive data.
- Log meaningful business and operational events without excessive noise.

## Security awareness
- Follow the `spring-security` skill for all authentication and authorization concerns.
- Default to safe behavior when exposing endpoints.
- Do not leave mutating endpoints unintentionally open.
- Keep business logic separate from security configuration.

## Testing awareness
- Write code that is easy to test.
- Keep responsibilities separated so service, controller, and repository tests remain straightforward.
- Favor maintainable code over code that is clever but hard to verify.

## Output expectations
When generating or refactoring code:
- use layered Spring Boot structure
- include DTOs for API boundaries
- include validation where input is accepted
- keep controllers thin
- keep services focused
- follow Maven conventions
- keep configuration and code production-oriented
- briefly explain any non-obvious design choices