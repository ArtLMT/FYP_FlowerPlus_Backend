---
type: adr
id: 3
status: accepted
date: 2026-09-13
module: infrastructure
aliases:
  - One schema per environment
tags:
  - flowerplus/adr
---

# ADR 0003 — One schema per environment

## Context

Every integration test deletes all refresh tokens, profiles and users before it runs
(`AuthIntegrationSupport`, `AddressIntegrationSupport`, and the frozen `AuthRegressionTest`).
All of them pointed at the same database and schema as the running app, so a test run erased
real data. On 2026-09-13 a single `RegistrationTest` run left `public` with one test user and
no admin.

The usual way to keep test data out is a **test-managed transaction**: a `@Transactional` test
rolls back at the end of each test method by default. That does not fit this codebase. Two
listeners run only after a commit:

- `EmailEventListener.onOtpRequested` — `@TransactionalEventListener(phase = AFTER_COMMIT)`,
  delivers the OTP email
- `OtpServiceImpl.onEmailVerified` — `@TransactionalEventListener(phase = AFTER_COMMIT)`,
  invalidates a used code

Spring invokes an `AFTER_COMMIT` listener only once the transaction has completed successfully.
A test transaction that is rolled back never commits, so no OTP would ever reach the fake mail
sender and every test that waits for a code would fail.

## Decision

**One database, `flowerplus`; one schema per environment, chosen by Spring profile.**

| Profile | Schema | Used by |
|---|---|---|
| `prod` | `public` | the real data |
| `dev` | `test` | the local app **and** every test run |

- `application.properties` reads the schema from `application.database.schema` for both the
  JDBC connection (`currentSchema`) and Flyway (`spring.flyway.default-schema`). The property
  has **no default**: with no profile active, startup fails instead of writing to a schema
  nobody chose.
- `application-dev.properties` and `application-prod.properties` each set only the schema.
- The local app selects its profile with `SPRING_PROFILES_ACTIVE=dev` in `.env`.
- Tests select `dev` through `src/test/resources/config/application.properties`. It exists only
  on the test classpath, and Spring Boot loads `classpath:/config/` after the classpath root, so
  it adds to the main file instead of replacing it. It also supplies the test admin, because
  `AuthRegressionTest` declares none. The frozen test is not edited.

## Consequences

**Gained**

- A test run never touches `public`.
- Tests keep real commit behaviour, so the after-commit listeners run exactly as in production.
- `AuthRegressionTest` stays unmodified.
- Profiles now exist, which is where other per-environment settings go later (Hardening Roadmap
  N6: `show-sql`, cookie `Secure`).

**Given up**

- The local app and the tests share `test`: every test run deletes the accounts created while
  running the app locally. Chosen knowingly on 2026-09-13.
- Forgetting `SPRING_PROFILES_ACTIVE` stops the app. IntelliJ run configurations that do not
  load `.env` need the variable set themselves.
- `public` currently holds only leftovers from runs before this change.

## Alternatives considered

- **Test-managed rollback (`@Transactional` tests)** — the after-commit listeners above would
  never fire.
- **A separate database (`flowerplus_test`)** — works; not preferred.
- **Testcontainers** — a throwaway Postgres per run; needs a new test dependency and Docker
  running for every test.
- **One `DB_SCHEMA` variable instead of profiles** — simpler today, but no home for other
  per-environment settings.
- **Fall back to `public` when no profile is set** — a forgotten setting would silently write
  to the real data.
- **Set the profile through Surefire in `pom.xml`** — applies only to `./mvnw` runs, not IntelliJ
  test runs, and edits `pom.xml`.

## Sources

- Spring Framework — test-managed transactions roll back by default:
  <https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/tx.html>
- Spring Framework — transaction-bound events:
  <https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html>
- Spring Boot — config file locations and precedence:
  <https://docs.spring.io/spring-boot/reference/features/external-config.html>
- Spring Boot — `spring.profiles.active` in non-profile-specific files:
  <https://docs.spring.io/spring-boot/reference/features/profiles.html>

---

Indexed in [[Decisions]].
