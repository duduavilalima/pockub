---
name: flyway-migrations
description: Use this skill when creating or reviewing Flyway database migrations in a Spring Boot project. Covers schema evolution, data migrations, zero-downtime strategies, and PostgreSQL best practices.
---

# Flyway Migrations

## Scope
Apply this skill when working on:
- Flyway migration scripts (`V1__`, `V2__`, etc.)
- schema evolution (DDL)
- data migrations (DML)
- production database changes
- PostgreSQL schema design

---

## Core Principles

- Every database change must be implemented via a migration — never modify production databases manually
- Migrations are **forward-only** — never edit applied migrations
- Separate **schema (DDL)** and **data (DML)** migrations
- Migrations must be **immutable once applied**
- Always test migrations on production-like data

---

## Flyway Naming & Structure

- Use versioned scripts:
    - `V1__init.sql`
    - `V2__add_user_table.sql`
- Use clear, descriptive names
- Keep each migration small and focused
- Avoid combining multiple concerns in one migration

---

## Migration Safety Rules

Before applying a migration:

- No full table locks on large tables
- New columns must be:
    - nullable OR
    - have default values
- Indexes must be created safely (see below)
- Large updates must be batched
- Rollback strategy must be known (even if forward-only)

---

## PostgreSQL Safe Patterns

### Adding Columns

```sql
-- SAFE
ALTER TABLE users ADD COLUMN avatar_url TEXT;

-- SAFE (Postgres 11+)
ALTER TABLE users ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT true;

-- UNSAFE
ALTER TABLE users ADD COLUMN role TEXT NOT NULL;