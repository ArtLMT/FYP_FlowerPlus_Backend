---
type: module
module: auth
phase: "00"
status: shipped; password reset added 2026-09-11, untested
aliases:
  - Auth module
tags:
  - flowerplus/module
---

# Module: Auth

Registration, login, Google login, refresh-token rotation and password reset. Requirements:
[[Auth rules]].

## Sign-in and sessions

Tokens travel **only** in two HttpOnly cookies — `flowerplus_at` (access, 24 h) and `flowerplus_rt`
(refresh, 7 days). No response body carries a token and no endpoint reads one from a body, so page
scripts can never read them (decided 2026-09-15).

| Endpoint | Request | Response |
|---|---|---|
| `POST /api/auth/login` | `{email, password}` | `204`, empty body, both cookies set |
| `POST /api/auth/verify-email` | `{email, code}` | `204`, empty body, both cookies set |
| `POST /api/auth/refresh` | the `flowerplus_rt` cookie | `204`, empty body, both cookies replaced; `401 REFRESH_TOKEN_INVALID` when the cookie is missing, unknown, reused or revoked |
| `POST /api/auth/logout` | the `flowerplus_rt` cookie | `204`, both cookies cleared |

- After signing in, the frontend calls `GET /api/users/me` for the user.
- A refresh token sent in a JSON body is ignored — the request counts as having no token.
- API calls may send the access token as the cookie or as `Authorization: Bearer`; `JwtFilter`
  accepts both.
- Google login sets the same two cookies and redirects; see `OAuth2AuthenticationSuccessHandler`.

## Password reset

| Endpoint | Body | Response |
|---|---|---|
| `POST /api/auth/forgot-password` | `{email}` | always `204` |
| `POST /api/auth/reset-password` | `{email, code, newPassword}` | `204`; `400 OTP_INVALID` for a wrong or expired code |

- Only ACTIVE and SUSPENDED accounts get a code. PENDING, BANNED and unknown emails get nothing,
  and the same `204`.
- The code is the same 6-digit code as registration (5 minutes, 5 attempts), namespaced by purpose,
  so a reset code can never verify a registration and vice versa.
- The email has the same layout as the verification email, with reset wording.
- A successful reset deletes all the account's refresh tokens and clears this browser's cookies.
  Access tokens already issued stay valid until they expire (`JWT_EXPIRATION`).
- New passwords follow the one password policy: 8–72 characters (`PasswordPolicy`).

## Staff first-password invitation

When an Admin creates a Staff account (user module, accounts P3), that module publishes
`StaffAccountCreatedEvent`. Auth's `StaffInvitationListener` reacts after commit, `@Async`, and
calls `OtpService.issueStaffInvitation`. That mints a code **stored under `PASSWORD_RESET`**, so the
ordinary `POST /api/auth/reset-password` sets the first password — but publishes
`StaffInvitationRequestedEvent`, which the email module delivers as a **welcome** mail ("set your
password"), not a reset one. Only the wording differs; the code and the endpoint are the reset flow.

- Spends the same send limits as a reset. A documented, tolerated edge: if a `forgot-password` was
  issued for that email in the 60 s before creation, the invitation is throttled — the account still
  exists and the person can use `forgot-password`. The failure is logged, never surfaced.
- The account is ACTIVE, so it is eligible for the reset that sets the password.

## Traps

- **The send limits apply to every request, account or not.** Emails that get no code still go
  through `OtpService.throttle`, so a quick second request gets the same `429` either way.
  Skipping that for unknown emails would make the `429` itself reveal registered addresses.
- **At most 5 reset codes per email per 24 hours** (`OTP_RESET_DAILY_LIMIT`), answered with
  `429 OTP_DAILY_LIMIT_REACHED` rather than the resend interval's `OTP_THROTTLED`. One code survives
  5 guesses, but a fresh code every 60 seconds would otherwise allow about 7,200 guesses a day
  against one account.
- **The account is re-checked after the code verifies** — it may have been banned since the code
  was sent. The answer is the same `400` as a wrong code.

## Tests

- `OtpServiceImplTest` — plain unit test, runs anywhere: resend interval, daily cap, purpose
  separation.
- `PasswordResetTest` — integration: the full flow, sessions ended, no code for ineligible accounts,
  the same `429` for unknown emails, wrong code, password policy. **Not yet run.**
- `RefreshRotationTest` — integration, tokens sent as the cookie: rotation, replay, reuse wipes the
  family, and a token sent only in a body is `401`.

Indexed in [[Modules]].
