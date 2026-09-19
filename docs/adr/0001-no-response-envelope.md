---
type: adr
id: 1
status: accepted
date: 2026-09-10
amended: 2026-09-13
module: api
aliases:
  - No response envelope
tags:
  - flowerplus/adr
---

# ADR 0001 — No response envelope

## Context

`common/dto/ApiResponse<T>` existed from early scaffolding: a wrapper carrying
`{ success, message, data, timestamp }`. It was referenced by no controller, no service and no test.
`UserController` returned bare DTOs.

Before writing the first list endpoint, this had to be settled one way or the other. Leaving an
unused wrapper in `common/` while seven more modules get written guarantees it eventually gets
adopted by half of them.

## Decision

**Delete `ApiResponse<T>`. Controllers return the resource itself, or nothing.**

Uniformity is provided by three things instead:

1. **The HTTP status code is the discriminator.** Every endpoint follows one table:

   | Situation | Status | Body |
   |---|---|---|
   | Read succeeded | `200` | the resource |
   | Created | `201` + `Location` | the created resource |
   | Succeeded, nothing to return | `204` | empty |
   | Validation failed | `400` | `ErrorResponse` |
   | Not authenticated | `401` | `ErrorResponse` |
   | Authenticated, not permitted | `403` | `ErrorResponse` |
   | Request the client built wrongly *(added 2026-09-15)* | `400` | `ErrorResponse` with `MALFORMED_REQUEST` |
   | No such row, or not the caller's | `404` | `ErrorResponse` |
   | No endpoint at the path, or an unparseable path id *(added 2026-09-15)* | `404` | `ErrorResponse` with `NOT_FOUND` |
   | Wrong HTTP method *(added 2026-09-15)* | `405` + `Allow` | `ErrorResponse` |
   | Conflicts with current state | `409` | `ErrorResponse` |
   | Body not JSON *(added 2026-09-15)* | `415` | `ErrorResponse` |
   | Too many requests *(added 2026-09-13)* | `429` | `ErrorResponse` with `details.retryAfterSeconds` where a wait applies |
   | Unexpected failure *(added 2026-09-13)* | `500` | `ErrorResponse` with `INTERNAL_ERROR` |

2. **One error shape, centrally produced.** `GlobalExceptionHandler` maps every exception in the
   application to `ErrorResponse`. Errors are the unpredictable half of an API — thrown from any
   module, on any path — so that is where a guaranteed common shape actually earns its keep.

3. **Conventions at the DTO layer.** Every response DTO is a record with a static `from(...)`; every
   paged endpoint returns `PageResponse<T>`; no entity ever reaches Jackson.

## Consequences

**Gained**

- `@PostAuthorize("returnObject.email == authentication.name")` in `UserController` keeps working.
  Wrapping the return type would have made `returnObject` an `ApiResponse<UserResponse>` and silently
  broken the SpEL path — re-plumbing an authorization check that was hardened in `89affe4` for a
  cosmetic gain.
- Tests assert `$.email` rather than `$.data.email`, across all remaining modules.
- No temptation toward the envelope anti-pattern of returning `200 OK` with `{"success": false}`,
  which makes every status-code-aware client, cache and monitor wrong.

**Given up**

- A client cannot parse success and failure with a single type. It must branch on status first.
  This is judged acceptable: every HTTP client library already exposes that branch, and
  `ApiResponse` and `ErrorResponse` had different field sets anyway, so the wrapper never actually
  delivered the single-shape benefit it is usually adopted for.
- If a frontend is later written against a `res.data.x` convention, it will need an unwrap shim.

## Alternatives considered

- **Adopt `ApiResponse<T>` everywhere.** Rejected for the reasons above.
- **RFC 9457 Problem Details** (`application/problem+json`), which Spring Boot supports natively via
  `ProblemDetail`. `ErrorResponse` is effectively a hand-rolled version of it. Not adopted now
  because changing the error shape would touch every existing auth test, but it is the obvious
  future direction and is recorded here so the choice is a choice.

---

## Amendment — 2026-09-13: error codes the frontend can act on

### Context

`ApiResponse<T>` was originally built so the frontend could react without reading logs or messages.
With the envelope gone, that job falls to `ErrorResponse.errorCode` — so the question became whether
the code alone lets the frontend respond correctly. An audit of every place a code is produced found
it mostly does, with four gaps:

1. **`OTP_THROTTLED` meant two different waits** — the 60-second resend interval and the daily cap on
   password-reset codes — and nothing said how long either lasted.
2. **`VALIDATION_FAILED` named the field but not the rule** — `validationErrors` holds an English
   message per field, so "required", "too short" and "bad format" looked alike.
3. **`message` is English only.** `ErrorCode` carries a `messageKey`, but no messages file exists and
   nothing reads it, so the frontend cannot get translated text from the backend.
4. **The Google login redirect used its own lowercase values** (`account_blocked`,
   `authentication_failed`) instead of `ErrorCode` names.

### Decision

**Still no envelope.** The error contract is `errorCode` plus, where the frontend must act
differently, a structured field — never the text of `message`:

- `OTP_DAILY_LIMIT_REACHED` is split from `OTP_THROTTLED`; both carry `retryAfterSeconds`, the real time
  left on the limit.
- `VALIDATION_FAILED` from a request body carries `validationRules` (field → constraint name, such as
  `Size` or `NotBlank`) beside `validationErrors`.
- The Google login redirect's `error` parameter uses `ErrorCode` names: `ACCOUNT_BLOCKED`, `UNAUTHENTICATED`.
- **The frontend owns all user-facing text**, keyed by `errorCode` (and the rule for validation).
  `message` stays English, for developers.
- Every code, when it happens and what the frontend should do is catalogued in
  `docs/error-codes.md` ([[error-codes]]).

### Consequences

**Gained**

- The frontend never parses a message to decide what to do or how long to wait.
- Purely additive: no existing field was renamed or removed, and the frozen `AuthRegressionTest` still
  passes unchanged (`OTP_THROTTLED` keeps its meaning for the resend interval).
- The throttle answers stay identical for registered and unregistered emails, so they still reveal
  nothing about which accounts exist.

**Given up**

- The frontend must keep a translation per code and per validation rule.
- A field that fails two constraints reports only one of them.
- `ErrorResponse` grows two fields that are `null` on most errors.

### Alternatives considered

- **Bring back an envelope for every response** (`{success, code, message, data}`). Would move every
  success body under `data`, break the frozen `AuthRegressionTest` (it asserted top-level
  `$.flowerplus_at`, `$.message`, `$.errorCode`; tokens left the body on 2026-09-15, see
  `docs/modules/auth.md`) and the `returnObject.email` check above.
- **One generic `details` object on `ErrorResponse`.** More flexible, but a frontend developer could not
  tell from the code catalogue what each error carries.
- **Translate `message` on the backend** through `messageKey`, a messages file and `Accept-Language`.
  Codes could stay coarse, but UI wording would live in Java instead of the frontend.

---

## Amendment 2 — 2026-09-13: a fixed frame plus typed `details`

### Context

The first amendment added `retryAfterSeconds` and `validationRules` as fields of the one flat
`ErrorResponse` class. Jackson writes every field of a class, so every error — a 500 included — then
carried three fields it had nothing to do with. Looking at why showed the frame was not uniform
either:

1. **The class was the contract.** Anything one error needed appeared on all of them.
2. **The frame was copied, not built.** Sixteen places set the same seven fields by hand.
3. **`timestamp` had two formats.** `JwtAuthenticationEntryPoint` and `JwtAccessDeniedHandler` built
   their own Jackson 2 `ObjectMapper`, which wrote it as a number (`1789286400.123`); everything else
   went through Spring's Jackson 3 mapper and wrote an ISO-8601 string. No test looked at it.
4. **The code did not decide the payload.** `VALIDATION_FAILED` had the maps for a bad body and none
   for a bad path parameter or malformed JSON.

This is not the envelope coming back. `ApiResponse<T>` put a frame around *successes*; this puts a
frame around *errors* only, where a response can come from any call. Successes stay bare.

### Decision

- **`ErrorResponse` is a record: a fixed frame and an optional `details`.** The frame is
  `success, status, error, errorCode, message, path, timestamp` on every error. `details` is omitted
  unless the code defines one.
- **`details` is one of a sealed set, `ErrorDetails`:** `Retry { retryAfterSeconds }` for the two OTP
  throttle codes, `Validation { fields: [{ field, rule, message }] }` for `VALIDATION_FAILED`.
  Supersedes the first amendment's rejection of "one generic `details` object": that was rejected
  because a frontend developer could not tell what it carries. A closed type per code, listed in
  [[error-codes]], answers that.
- **Every body is built by `ErrorResponse.of(code, message, path[, details])`**, which derives
  `success`, `status`, `error` and `timestamp` from the code.
- **The security handlers use Spring's `JsonMapper`.** No production code builds its own mapper.
- **`error` is the HTTP reason phrase** — the same information as `status` — and **`success` is always
  `false`**. Both are kept only because the frozen `AuthRegressionTest` asserts them on the 401.
- **`message` stays**, English, for developers: what happened this time, which `error` cannot say.

### Consequences

**Gained**

- An error without details has no unrelated fields. A frontend can type errors as a union keyed on
  `errorCode`, where `details` needs no null check.
- `timestamp` is an ISO-8601 string on every path.
- A field failing two constraints now reports both.
- `AuthRegressionTest` unchanged: the six fields it asserts stay at the top level.

**Given up**

- `retryAfterSeconds`, `validationErrors` and `validationRules` moved or went. They were one day old
  and no frontend code read them.
- `error` and `success` stay as redundant fields.

**Resolved 2026-09-15**

- Malformed JSON and a bad path parameter used to answer `VALIDATION_FAILED` without `details`. Now
  broken JSON is `MALFORMED_REQUEST` (400) and an unparseable path id is `NOT_FOUND` (404), so
  `VALIDATION_FAILED` always carries `fields`.
- An unknown path, a wrong method and a non-JSON body were reported as `500 INTERNAL_ERROR` with a
  stack trace, because the catch-all handler caught Spring's routing exceptions first (reproduced on
  the running app). They are now `404 NOT_FOUND`, `405 METHOD_NOT_ALLOWED` and
  `415 UNSUPPORTED_MEDIA_TYPE`, logged as one WARN line.

### Alternatives considered

- **`@JsonInclude(NON_NULL)` on the flat class.** Hides the nulls in JSON, but the Java type still
  says every error may carry every field.
- **RFC 9457 `ProblemDetail`.** Renames `message` to `detail`, which breaks the frozen test.

---

Indexed in [[Decisions]].
