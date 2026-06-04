---
name: java-best-practices
description: Use this skill when generating or reviewing Java code to ensure readability, maintainability, correctness, and modern best practices.
---

# Java Best Practices

## Scope
Apply this skill when working on:
- service and domain logic
- utility classes
- refactoring existing code
- general Java design and structure

---

## Code clarity and readability
- Use intention-revealing names for classes, methods, and variables.
- Keep methods small and focused on a single responsibility.
- Avoid deep nesting; prefer early returns.
- Prefer readability over clever or overly compact code.

---

## Null safety
- Avoid NullPointerException risks in chained calls.
- Prefer Optional for nullable return values.
- Return empty collections instead of null.
- Validate inputs explicitly.

---

## Exception handling
- Do not swallow exceptions.
- Avoid catching overly broad exceptions (Exception, Throwable).
- Always preserve the original cause when rethrowing.
- Log with context and stack trace.

---

## Separation of concerns
- Keep business logic separate from persistence and transport layers.
- Avoid large “god classes”.
- Keep classes cohesive and focused.

---

## Collections and streams
- Do not modify collections while iterating.
- Use streams for transformations, loops for side effects.
- Avoid unnecessary stream usage in tight loops.
- Be explicit about mutability when collecting results.

---

## Dependency and design principles
- Prefer constructor injection.
- Make dependencies explicit.
- Avoid hidden or implicit dependencies.
- Design classes for clarity and testability.

---

## Resource management
- Use try-with-resources for AutoCloseable resources.
- Ensure all I/O and database resources are properly closed.

---

## Concurrency awareness
- Avoid shared mutable state when possible.
- Use thread-safe collections when needed.
- Prefer immutable objects.
- Be careful with async and multi-threaded code.

---

## Performance awareness
- Avoid obvious performance anti-patterns:
    - string concatenation in loops
    - repeated regex compilation
    - unnecessary object creation in hot paths
- Prefer readability first; optimize only when necessary.
- Measure before optimizing.

---

## Refactoring mindset
- Refactor when duplication appears.
- Simplify complex logic into smaller units.
- Improve naming and structure when readability suffers.
- Prefer incremental improvements.

---

## Testability
- Write code that is easy to test.
- Avoid tight coupling.
- Keep business logic independent from frameworks when possible.
- Prefer deterministic behavior.

---

## Output expectations
When generating or refactoring code:
- prioritize clarity and maintainability
- avoid unnecessary abstraction
- follow modern Java idioms
- improve existing code where possible
- explain non-obvious improvements briefly