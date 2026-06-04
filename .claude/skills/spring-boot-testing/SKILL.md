---
name: spring-boot-testing
description: Use this skill when writing or improving tests in a Spring Boot project, including unit tests, integration tests, and test structure.
---

# Spring Boot Testing

## Scope
Apply this skill when working on:
- unit tests for services and components
- integration tests for Spring Boot applications
- controller tests
- improving test coverage and quality

---

## Testing philosophy
- Write meaningful, maintainable tests.
- Focus on behavior, not implementation details.
- Cover both happy paths and edge cases.
- Keep tests deterministic and isolated.

---

## Frameworks
- Use JUnit 5 (Jupiter).
- Prefer AssertJ for assertions.

```java
assertThat(result)
    .isNotNull()
    .hasFieldOrPropertyWithValue("status", "ACTIVE");
```

## Test types and strategy

Use the appropriate test type based on scope:

### Unit tests
- Test service and utility logic in isolation.
- Do not start Spring context.
- Use mocks (e.g., Mockito) for dependencies.

### Slice tests (recommended for most cases)
- Load only a part of the Spring context.
- Prefer slice tests as the default testing approach in Spring Boot applications.

Common slices:
- `@WebMvcTest` → controller layer
- `@DataJpaTest` → repository layer

Benefits:
- faster than full context
- focused and isolated

### Integration tests
- Use `@SpringBootTest` when:
    - testing full application behavior
    - verifying configuration and wiring
- Combine with:
    - `@AutoConfigureMockMvc` for API testing
    - Testcontainers for real database testing

---

## Test isolation and data

- Prefer in-memory or containerized databases for tests.
- Reset state between tests.
- Avoid shared mutable state.
- Keep tests deterministic and repeatable.

---

## What to prefer

- Prefer slice tests over full `@SpringBootTest` where possible.
- Use full context tests only when necessary.
- Keep test execution fast to support frequent runs.


