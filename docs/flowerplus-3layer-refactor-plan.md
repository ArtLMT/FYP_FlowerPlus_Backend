# FlowerPlus: Hexagonal → 3-Layer Refactor Plan (v2)

**Decision:** package-by-feature 3-layer for all modules. The four existing hexagon modules
converge onto that standard; the remaining eleven are built in it from the start.

**Scope split — keep these separate in your head:**

| Half | What it buys | Droppable? |
|---|---|---|
| Build modules 5–15 as 3-layer | ~150–200 fewer files, 2 edit points per new operation instead of 5 | **No.** Free, and compounds. |
| Convert auth / user / email / common | One standard, one viva answer | **Yes.** If a sprint slips, this is what gets cut. |

Estimated total: **~2.5 focused days**. Estimates per phase are rough and assume no surprises.

**Rule, written once and applied everywhere:**

> Package by feature. Inside each feature: `web` → `service` → `repository`/`entity`.
> Declare an interface only where a second implementation genuinely exists, or where
> faking it is how you test.

```
module/<feature>/
  web/            Controller + dto/ (request & response records)
  service/        Services, result records, module exceptions
  repository/     Spring Data JPA interfaces
  entity/         JPA entities (rich — behaviour lives here)
  infrastructure/ Non-JPA external adapters (Redis, SMTP) — only when needed
```

**Interfaces that survive:** `OtpStore` (auth), `EmailSender` (email); later `PaymentGateway`,
`ImageStorage`, `NotificationChannel`. Five, not fifty.

---

## Decisions needed before Phase 0 closes

Two are behaviour questions I can't answer for you. Both block Phase 0 step 2.

**D1 — What does a BANNED user see when they try to re-register?**
`RegisterService.register` branches on `ACTIVE` (throw) and `PENDING` (resend). `SUSPENDED` and
`BANNED` match neither, so execution falls through to `UserJpaEntity.builder()` and attempts a
second row with the same email → unique constraint violation surfacing as a 500. Pick one:
generic "email already in use" (leaks nothing), or an explicit banned message (better UX, enables
enumeration). Whichever you pick, `SUSPENDED` and `BANNED` both need an explicit branch.

**D2 — Is "SUSPENDED can log in, BANNED cannot" the rule you want?**
That's what `SecurityUser.isAuthBlocked` currently produces, and your verification checklist
asserts it as the baseline. Confirm it's intentional before you freeze it into a test.

Non-blocking, decide as you go:

- **D3** — Standardise on dirty checking (drop the redundant explicit `save` calls inside
  `@Transactional`)? My recommendation: yes, with a one-line comment at the first occurrence.
- **D4** — `common/` enums: move `OrderStatus`, `PaymentStatus`, `VoucherStatus` etc. into their
  modules as each lands, or keep `common` as shared vocabulary? Real argument for keeping:
  `order_details` references product *and* material. Defer — it costs nothing to decide later.
- **D5** — Does `ApplicationConfig` route through `UserService` instead of `UserJpaRepository`?
  `security/` is cross-cutting, so reaching into user persistence is more defensible there.
  Leaving it is fine; note the decision in the report so it doesn't look like an oversight.
- **D6** — `InventoryType` → `MaterialType` rename is staged but uncommitted. The schema calls
  the table `inventory_item`; your domain model calls it `Material`. Settle the vocabulary before
  rebuilding that module — it names a table, a module, and ~6 classes. Also: `Material`'s fields
  (`unitOfMeasure`, `perishable`, `shelfLife`, `status`) don't exist on `inventory_item` in V1, so
  the rebuild needs a `V5__` migration.

---

## Phase 0 — Safety and prerequisites (~2h)

Nothing here is refactoring. All of it happens on `main`, before the refactor branch exists.

**0.1 — Rescue the material module.**
`module/material/` is entirely untracked. Deleting untracked files is unrecoverable, and
`Material.java` is a pure domain model with your BR-MAT-xx invariants — there is nothing
hexagonal in it to refactor. It is **not** deleted; it moves into the new layout as-is.

```bash
git checkout -b wip/material-domain-snapshot
git add -A && git commit -m "wip: material domain model before 3-layer refactor"
git checkout main
```

**0.2 — Fix the two behaviour bugs, on `main`, in their own commits.**
These are behaviour changes. If they live inside a rename commit and something breaks later, you
cannot tell which change did it — and the whole refactor's guarantee is "behaviour is identical."

- The `RegisterService` SUSPENDED/BANNED fall-through (per **D1**).
- `EmailService.sendOTP` hardcodes `expiryMinutes: 5` while the real TTL is `OtpProperties.ttl()`
  (`application.properties:59`). Inject `OtpProperties` and derive it. Change `OTP_TTL` today and
  the email tells the user the wrong number.

**0.3 — Pre-flight greps.** These are the two ways a "no schema change" refactor silently changes
your schema.

```bash
# Every @Entity must have an explicit @Table(name = ...).
# Without it, renaming UserJpaEntity → User renames the table.
grep -rn -A3 "@Entity" --include=*.java src/main | grep -B1 -A2 "@Table" 

# Any @Query using JPQL entity names will break on rename (fails at boot, not runtime).
grep -rn "@Query" --include=*.java src/main
```

Fix any entity missing `@Table` **before** Phase 2, as its own commit.

**0.4 — Verify the POM.** The base plan claimed `spring-boot-starter-webmvc-test` and
`spring-boot-starter-data-jpa-test` are present and unused. Those artifact IDs don't exist —
the real one is `spring-boot-starter-test`, which bundles JUnit 5, Mockito, AssertJ and MockMvc.
Confirm what's actually there; you'll need it in Phase 6, plus:

```xml
<dependency>
  <groupId>com.tngtech.archunit</groupId>
  <artifactId>archunit-junit5</artifactId>
  <scope>test</scope>
</dependency>
```

**0.5 — Branch and baseline.**

```bash
git checkout -b refactor/3-layer
./mvnw clean compile   # must be green before you touch anything
```

---

## Phase 0.5 — Characterization harness (~2h)

This is the highest-value two hours in the plan. Without it, "behaviour must be identical" is an
aspiration you'll check once, manually, at the end, across six commits you can't bisect.

Write `scripts/characterize.sh` — curl through the full auth flow, dumping each complete response
(status, headers, body) to `characterization/$1/<step>.txt`. Steps:

1. `POST /api/auth/register` → 200, account PENDING
2. `POST /api/auth/verify-email` with the code → 200 + both cookies set, account ACTIVE
3. Wrong code ×6 → attempt-cap message, code destroyed
4. `POST /api/auth/resend-otp` inside 60s → 204 (throttle is silent), no second mail
5. `POST /api/auth/login` → 200 + cookies
6. `GET /api/users/{id}` with that JWT → 200
7. `GET /api/users/{id}` without a JWT → 401 in the `ErrorResponse` shape
8. `POST /api/auth/refresh` via cookie **and** via body → both 200
9. `POST /api/auth/logout` → 204, cookies cleared, that refresh token now 401s
10. Login as SUSPENDED / BANNED / PENDING accounts → per **D2**; expect success,
    `LockedException`, `DisabledException` respectively
11. `redis-cli KEYS "otp:*"` → `otp:register:{email}` and `otp:resend:{email}` with TTLs

Normalize the volatile bits (timestamps, JWTs, UUIDs) with `sed` so diffs are meaningful.

```bash
docker compose up -d && ./mvnw spring-boot:run &
./scripts/characterize.sh before
# ...after each phase:
./scripts/characterize.sh after && diff -r characterization/before characterization/after
```

Thirty seconds per phase instead of a manual Postman run. In Phase 6 this becomes real MockMvc
tests; for now the diff is enough.

---

## Phase 1 — Delete dead code (~1h, zero risk)

Nothing here is referenced anywhere. Verify each with a usage search before deleting.

- `common/dto/ApiResponse.java` — 0 usages; controllers return DTOs directly
- `UserAccountPort.existsByEmail` + adapter impl — never called (and implemented as
  `findByEmail().isPresent()`, loading a full row to test existence)
- `LoadUserPort.loadByEmail` + impl — never called
- `User.isActive()`, `User.displayName()` — never called
- `EmailEventListener`'s injected `TemplateEngine` field — unused (`SpringMailAdapter` templates)
- The unused `HttpServletResponse response` parameter on `AuthController.register`

`module/material/` is **not** in this list. It was in v1 of the plan; it stays.

Commit. `./mvnw clean compile` green. No characterization run needed — nothing executed changed.

---

## Phase 2 — `module/user` (~3h)

Smallest module, sets the template. But be aware: this is not the low-risk warm-up the base plan
called it. Renaming `UserJpaEntity` ripples into `module/auth` **and** `security/`. It's the
load-bearing edit. Let the compiler drive it.

**Delete:** `domain/model/User`, `domain/model/UserProfile`, `UserPersistenceMapper`,
`UserPersistenceAdapter`, `LoadUserPort`, `GetUserUseCase`, `UserBeanConfig`.

**Rename into the new layout:**

| From | To |
|---|---|
| `UserJpaEntity` | `entity/User` |
| `UserProfileJpaEntity` | `entity/UserProfile` |
| `UserJpaRepository` | `repository/UserRepository` |
| `UserProfileJpaRepository` | `repository/UserProfileRepository` |

`UserService` gains `@Service` and constructor-injects both repositories. `UserController` maps
to `UserResponse` — keep `UserResponse.from(...)`, retarget it at the entity.

**Two guards, added here, before the pattern spreads to eleven more modules:**

- `@JsonIgnore` on the password hash field of `entity/User`. Your accepted trade-off is
  "services return entities, controllers always map to a DTO first" — enforced by discipline,
  with no tests, across twelve modules. One forgotten mapping leaks credentials in a demo. The
  annotation costs nothing and makes the failure impossible rather than unlikely.
- `@Transactional(readOnly = true)` at **class** level on read-heavy services, overridden per
  write method. Easier to be right by default than to remember each time. This matters now that
  lazy associations can reach the controller.

Commit. Compile. Run the characterization diff — this is the first phase that can change behaviour.

---

## Phase 3 — `module/email` (~1.5h)

**Delete:** `SendEmailUseCase`.

| From | To |
|---|---|
| `EmailSenderPort` | `service/EmailSender` *(survives — you can't send real mail in a test)* |
| `SpringMailAdapter` | `infrastructure/SpringMailSender` |
| `EmailMessage` | `service/EmailMessage` — drop `@Setter`, it's built once and read once |

`EmailEventListener` stays in `infrastructure/`, depends on `EmailService` directly.

Convert `@Value` field injection to constructor injection in `SpringMailSender`
(`application.mail.from`) — everything else in the codebase uses constructor injection.

The OTP expiry bug is already fixed in Phase 0.2. Don't fix it again here.

Commit. Compile. Characterization diff (the OTP email path is exercised by steps 1–4).

---

## Phase 4 — `module/auth` (~6h, biggest phase)

**Delete:** all 8 in-ports; `UserAccountPort`, `RefreshTokenPort`, `TokenIssuerPort`,
`PasswordEncoderPort`; adapters `UserAccountAdapter`, `JwtTokenAdapter`, `PasswordEncoderAdapter`.

`PasswordEncoderPort` wraps Spring's `PasswordEncoder`, which is already an interface with
`encode` and `matches`. The port adds a rename and a class. Inject `PasswordEncoder` directly.

**Collapse 7 single-method services into 4 — not 1.** A service-per-use-case is a hexagon habit;
one service for the whole module is the opposite overcorrection. Multiple cohesive services inside
`service/` collaborating with each other is ordinary 3-layer:

| Service | Absorbs | Injects |
|---|---|---|
| `service/AuthService` | `RegisterService`, `LoginService`, `LogoutService`, `RefreshService` | `UserService`, `OtpService`, `RefreshTokenService`, `PasswordEncoder`, `JwtService`, `AuthenticationManager` |
| `service/EmailVerificationService` | `VerifyEmailService`, `ResendOtpService` | `UserService`, `OtpService` |
| `service/OtpService` | unchanged | `OtpStore`, `PasswordEncoder` |
| `service/RefreshTokenService` | `RefreshTokenAdapter`'s **logic** | `RefreshTokenRepository` |

**`RefreshTokenAdapter` splits.** Persistence goes to `repository/RefreshTokenRepository` (exists
as `RefreshTokenJpaRepository`). The revoked / expiry / delete rules — currently in an
infrastructure class throwing an API-layer `UnauthorizedException` — move into
`RefreshTokenService` where `@Transactional` belongs. While moving: `verify()` currently deletes
rows on the expiry path **without** `@Transactional`, unlike `create` and `revoke`. Give it one.

**`RedisOtpAdapter` → `infrastructure/RedisOtpStore`**, implementing the surviving `OtpStore`.
Otherwise unchanged — it's the best-designed class in auth. Fix the garbled comment in `save`
(currently reads `"couleave the oldnt in place"`).

**Cross-module boundary — this is the actual fix for the port leak.** `AuthService` depends on
`UserService`, never on `UserRepository`. `UserService` gains the methods auth needs:
`findByEmail`, `createPendingAccount`, `activate`, `updateProfileName`.

Note what this means and put it in your report: you are deciding that **the user module owns
account lifecycle**. Otherwise you've relocated the coupling rather than removed it. It's a
defensible decision — just make it deliberately.

**Service signatures:** services take primitives or entities and return entities or small result
records. `AuthService.login(String email, String rawPassword)` returns a
`TokenPair(String accessToken, String refreshToken)` record in `service/`; `AuthController`
builds `AuthResponse`.

**`AuthController`:** extract the duplicated 6-line cookie-vs-body token block (identical in
`refresh` and `logout`) into one private helper.

Commit. Compile. **Characterization diff is mandatory here** — this phase touches every
high-risk path. Pay particular attention to steps 8–10.

---

## Phase 5 — Cross-cutting tidy (~2h)

- **Delete the teaching javadoc.** `UserService`, `UserController`, `UserJpaEntity`,
  `UserResponse` and the now-deleted port/adapter/config classes carry "INSIDE the wall" /
  "OUTSIDE the wall" narration that is **false** after this refactor. An examiner reading
  "this keeps all Spring wiring OUT of the core" next to an `@Service` annotation is a bad
  moment. The architecture rationale belongs in the report, not in code comments.
  (`UserPersistenceMapper`'s javadoc already references a `getActualUsername()` workaround that
  no longer exists, and `UserBeanConfig`'s says it constructs "the LoginService" when it
  constructs `UserService` — evidence the comments were never load-bearing.)
- Constructor injection for the remaining `@Value` field injection in `RefreshTokenService`
  (`refresh-expiration`).
- **Move `OtpRequestedEvent`** out of `module/email/application/event`. It's published by auth
  and consumed by email; the publisher owns it → `module/auth/event/OtpRequestedEvent`.
- **Make the auth cookie `secure` flag config-driven.** `AuthController` hardcodes
  `.secure(false)` in four places with a `// Set to true in production/HTTPS` comment. You are
  deploying to a VPS — this one will bite you.
- Apply **D3** (dirty checking) consistently across the auth services, with a comment at the
  first occurrence saying it's the standard.

Commit. Compile. Characterization diff.

---

## Phase 6 — Lock it in (~3h)

The refactor removed your compile-time module boundaries. Replace them with tested ones, or
nothing stops `OrderService` from injecting `ProductRepository` in sprint 3 except you
remembering not to. At twelve modules and twenty-seven tables, that's the decay that actually
produces unmaintainable code — worse than anything hexagonal was costing you.

**6.1 — `ArchitectureTest` (~30 lines).** ArchUnit is not a hexagon tool; point it at module
boundaries rather than layer purity and it enforces the thing that matters in a modular monolith:

- no class outside `module.<x>` may depend on `module.<x>.repository..`
- no class outside `module.<x>` may depend on `module.<x>.entity..`
  *(if you'd rather let entities cross module lines for DTO mapping, drop this rule and say so —
  but then the `@JsonIgnore` guard from Phase 2 is doing real work)*
- `..service..` may not depend on `..web..`
- classes in `..repository..` must be interfaces annotated `@Repository` or extending
  `JpaRepository`

This is your answer to the viva question you *will* get — "you removed your architectural
boundaries, what enforces them now?" Compile-time boundaries became tested boundaries, and here
is the test. That's a stronger story than the hexagon one.

**6.2 — `OtpServiceTest`** with an in-memory `OtpStore` fake: throttling, attempt cap,
invalidation-on-exceeded, wrong-code rejection. No database, no Redis. This is the test the ports
were bought for and never written — write it now and the whole "testability was never cashed in"
argument is settled.

**6.3 — `SecurityUserTest`** asserting the documented invariant that `isAuthBlocked` equals the
union of `!isAccountNonLocked()` and `!isEnabled()` — currently stated in a comment, enforced by
nothing. Encode **D2** here.

**6.4 —** Convert the highest-value characterization steps (login, refresh, logout) to MockMvc
slice tests so they survive past this refactor.

`./mvnw test` green, and no longer just the one context-load test.

---

## Verification (run at every phase from 2 onward)

1. `./mvnw clean compile` — green
2. `./scripts/characterize.sh after && diff -r characterization/before characterization/after`
   — no diff outside normalized fields
3. **Flyway:** this refactor touches no schema. Confirm V1/V2/V4 replay on a fresh volume and
   `flyway_schema_history` checksums are untouched. If a checksum moved, an entity lost its
   `@Table` — go back to Phase 0.3.
4. `./mvnw test` — green (meaningful from Phase 6)

---

## If a sprint slips

Stop after Phase 4 and skip Phases 5–6's polish. Phases 0–4 leave you with one consistent
standard; 5 is cosmetics plus one real security fix (the cookie flag — do that one regardless),
and 6 is the payoff you can add later.

If it slips harder: stop after Phase 0 and build modules 5–15 in 3-layer without converting the
existing four. You'd carry two package layouts for a while, which is ugly but not fatal — and
after the port deletions the difference is `application/` vs `service/`, which is a rename, not
an architecture.

What does **not** get cut: Phase 0.2 (the two bugs), Phase 0.3 (the `@Table` check), and the
`secure` cookie flag.

---

## Where the recovered sprint goes

Four things in your schema are each worth more to an examiner than the entire architecture
chapter:

1. **Concurrent stock allocation.** Two orders for the last three roses arriving together.
   Pessimistic lock, or optimistic with retry — and you can *demonstrate the race and the fix*.
   The single most impressive thing available in your schema.
2. **Payment webhook idempotency.** Gateways retry. `financial_transaction` must not double-write.
3. **Order state machine.** You have `order_status_history`; enforce legal transitions rather
   than free-form status writes.
4. **Recipe explosion and its reversal.** `product_recipe` → deduct each `inventory_item` →
   write `inventory_transaction` + `order_inventory_allocation`, atomically, correctly undone on
   cancel.

Nobody will ask whether your service imported `@Service`. They will ask what happens when two
customers buy the last rose at the same time.
